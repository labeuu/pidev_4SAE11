package org.example.contract.service;

import org.example.contract.entity.Comment;
import org.example.contract.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void getByConflictId() {
        when(commentRepository.findByConflictIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        assertEquals(0, commentService.getByConflictId(1L).size());
    }

    @Test
    void createSetsConflictId() {
        Comment c = new Comment();
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        Comment out = commentService.create(7L, c);
        assertEquals(7L, out.getConflictId());
    }

    @Test
    void updateContent() {
        Comment db = new Comment();
        db.setId(2L);
        when(commentRepository.findById(2L)).thenReturn(Optional.of(db));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("hi", commentService.update(2L, "hi").getContent());
    }

    @Test
    void updateThrowsWhenMissing() {
        when(commentRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> commentService.update(9L, "x"));
    }

    @Test
    void deleteDelegates() {
        commentService.delete(4L);
        verify(commentRepository).deleteById(4L);
    }
}
