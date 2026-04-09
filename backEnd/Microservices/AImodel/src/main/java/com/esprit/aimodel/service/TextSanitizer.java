package com.esprit.aimodel.service;

import java.time.LocalDate;
import java.time.ZoneOffset;

public final class TextSanitizer {

    private TextSanitizer() {}

    public static String stripModelNoise(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl != -1) {
                s = s.substring(nl + 1);
                int end = s.lastIndexOf("```");
                if (end > 0) {
                    s = s.substring(0, end);
                }
            }
        }
        return s.trim();
    }

    private static String todayYyyyMmDd() {
        return LocalDate.now(ZoneOffset.UTC).toString();
    }

    public static String taskPlannerPromptPrefix() {
        String today = todayYyyyMmDd();
        return "You are a task planner. Return ONLY valid JSON with no markdown fences or commentary.\n"
                + "The JSON must be an object with a single key \"tasks\" whose value is an array of objects.\n"
                + "Each task object must have these string fields: \"title\", \"description\", \"priority\" (one of \"low\", \"medium\", \"high\"), and \"dueDate\" (deadline as yyyy-MM-dd).\n"
                + "Scheduling: today is "
                + today
                + ". Use dueDate values on or after "
                + today
                + ". Spread deadlines across the next few weeks (not all the same day) according to realistic effort order.\n"
                + "Example shape: {\"tasks\":[{\"title\":\"...\",\"description\":\"...\",\"priority\":\"medium\",\"dueDate\":\""
                + today
                + "\"}]}\n"
                + "\n"
                + "User context to plan tasks for:\n";
    }

    public static String subtaskPlannerPromptPrefix() {
        String today = todayYyyyMmDd();
        return "You are a task breakdown assistant. Return ONLY valid JSON with no markdown fences or commentary.\n"
                + "The JSON must be an object with a single key \"subtasks\" whose value is an array of objects.\n"
                + "Each subtask object must have these string fields: \"title\", \"description\", \"priority\" (one of \"low\", \"medium\", \"high\"), and \"dueDate\" (yyyy-MM-dd).\n"
                + "Break the parent task into concrete, ordered subtasks a freelancer can execute. Use chronological dueDate values on or after "
                + today
                + ", spaced by a few days when logical.\n"
                + "Example shape: {\"subtasks\":[{\"title\":\"...\",\"description\":\"...\",\"priority\":\"medium\",\"dueDate\":\""
                + today
                + "\"}]}\n"
                + "\n"
                + "Parent task and context to decompose:\n";
    }
}
