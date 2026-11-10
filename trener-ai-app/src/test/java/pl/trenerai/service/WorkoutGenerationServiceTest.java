package pl.trenerai.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WorkoutGenerationServiceTest {

    @Test
    void trainingGoalFromValidId() {
        var goal = WorkoutGenerationService.TrainingGoal.fromId("improve_ftp");
        assertEquals("zwiększenie FTP", goal.label());
        assertFalse(goal.promptInstruction().isEmpty());
    }

    @Test
    void trainingGoalFromInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> WorkoutGenerationService.TrainingGoal.fromId("nonexistent"));
    }

    @Test
    void allTrainingGoalsHaveNonEmptyLabelsAndPrompts() {
        for (var goal : WorkoutGenerationService.TrainingGoal.values()) {
            assertNotNull(goal.label());
            assertFalse(goal.label().isBlank());
            assertNotNull(goal.promptInstruction());
            assertFalse(goal.promptInstruction().isBlank());
        }
    }
}
