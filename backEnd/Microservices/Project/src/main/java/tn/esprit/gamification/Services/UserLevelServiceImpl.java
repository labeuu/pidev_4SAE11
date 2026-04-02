package tn.esprit.gamification.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Repository.UserLevelRepository;

import java.util.List;

@Service
public class UserLevelServiceImpl implements UserLevelService {

    @Autowired
    private UserLevelRepository repo;

    @Override
    public UserLevel getUserLevel(Long userId) {
        return repo.findByUserId(userId)
                .orElseGet(() -> {
                    UserLevel ul = new UserLevel();
                    ul.setUserId(userId);
                    return repo.save(ul);
                });
    }

    @Override
    public void addXp(Long userId, int xp) {
        UserLevel ul = getUserLevel(userId);

        ul.setXp(ul.getXp() + xp);

        int level = ul.getXp() / 100 + 1;
        ul.setLevel(level);

        repo.save(ul);
    }
    @Override
    public void incrementFastResponderStreak(Long userId) {
        UserLevel ul = getUserLevel(userId);
        ul.setFastResponderStreak(ul.getFastResponderStreak() + 1);
        repo.save(ul);
    }

    @Override
    public void resetFastResponderStreak(Long userId) {
        UserLevel ul = getUserLevel(userId);
        ul.setFastResponderStreak(0);
        repo.save(ul);
    }

    @Override
    public List<UserLevel> getAllUserLevels() {
        return repo.findAll();
    }

    @Override
    public List<UserLevel> getCurrentTopFreelancers() {
        return repo.findByIsTopFreelancerTrue();
    }

    @Override
    public void setTopFreelancer(Long userId, boolean status) {
        UserLevel ul = getUserLevel(userId);
        ul.setTopFreelancer(status);
        repo.save(ul);
    }
}
