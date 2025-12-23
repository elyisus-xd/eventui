package com.eventui.core.mission;

import com.eventui.common.dto.Result;
import com.eventui.common.model.MissionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MissionStateMachineTest {
    private MissionStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new MissionStateMachine();
    }

    @Test
    void testValidTransition_LockedToAvailable() {
        Result<MissionState> result = stateMachine.transition(
                MissionState.LOCKED,
                MissionState.AVAILABLE
        );

        assertTrue(result.isSuccess());
        assertEquals(MissionState.AVAILABLE, result.unwrap());
    }

    @Test
    void testValidTransition_AvailableToActive() {
        Result<MissionState> result = stateMachine.transition(
                MissionState.AVAILABLE,
                MissionState.ACTIVE
        );

        assertTrue(result.isSuccess());
        assertEquals(MissionState.ACTIVE, result.unwrap());
    }

    @Test
    void testValidTransition_ActiveToCompleted() {
        Result<MissionState> result = stateMachine.transition(
                MissionState.ACTIVE,
                MissionState.COMPLETED
        );

        assertTrue(result.isSuccess());
        assertEquals(MissionState.COMPLETED, result.unwrap());
    }

    @Test
    void testValidTransition_ActiveToFailed() {
        Result<MissionState> result = stateMachine.transition(
                MissionState.ACTIVE,
                MissionState.FAILED
        );

        assertTrue(result.isSuccess());
    }

    @Test
    void testValidTransition_ActiveToAvailable_Abandon() {
        Result<MissionState> result = stateMachine.transition(
                MissionState.ACTIVE,
                MissionState.AVAILABLE
        );

        assertTrue(result.isSuccess());
    }

    @Test
    void testInvalidTransition_LockedToActive() {
        Result<MissionState> result = stateMachine.transition(
                MissionState.LOCKED,
                MissionState.ACTIVE
        );

        assertTrue(result.isFailure());
        assertTrue(result.getError().contains("Invalid state transition"));
    }

    @Test
    void testInvalidTransition_AvailableToCompleted() {
        Result<MissionState> result = stateMachine.transition(
                MissionState.AVAILABLE,
                MissionState.COMPLETED
        );

        assertTrue(result.isFailure());
    }

    @Test
    void testInvalidTransition_CompletedToActive() {
        Result<MissionState> result = stateMachine.transition(
                MissionState.COMPLETED,
                MissionState.ACTIVE
        );

        assertTrue(result.isFailure());
    }

    @Test
    void testCanActivate_Available() {
        assertTrue(stateMachine.canActivate(MissionState.AVAILABLE));
    }

    @Test
    void testCanActivate_NotAvailable() {
        assertFalse(stateMachine.canActivate(MissionState.LOCKED));
        assertFalse(stateMachine.canActivate(MissionState.ACTIVE));
        assertFalse(stateMachine.canActivate(MissionState.COMPLETED));
    }

    @Test
    void testCanAbandon_Active() {
        assertTrue(stateMachine.canAbandon(MissionState.ACTIVE));
    }

    @Test
    void testCanAbandon_NotActive() {
        assertFalse(stateMachine.canAbandon(MissionState.LOCKED));
        assertFalse(stateMachine.canAbandon(MissionState.AVAILABLE));
        assertFalse(stateMachine.canAbandon(MissionState.COMPLETED));
    }
}
