package com.clinic.appointment_service;

import com.clinic.appointment_service.entity.AvailabilityException;
import com.clinic.appointment_service.entity.DoctorSchedule;
import com.clinic.appointment_service.entity.TimeSlot;
import com.clinic.appointment_service.repository.AvailabilityExceptionRepository;
import com.clinic.appointment_service.repository.DoctorScheduleRepository;
import com.clinic.appointment_service.repository.TimeSlotRepository;
import com.clinic.appointment_service.service.availability.SlotGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotGenerationServiceTests {

    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private DoctorScheduleRepository scheduleRepository;
    @Mock private AvailabilityExceptionRepository exceptionRepository;

    private SlotGenerationService service;

    @BeforeEach
    void setUp() {
        service = new SlotGenerationService(timeSlotRepository, scheduleRepository, exceptionRepository);
    }

    @Test
    void generatesSlotsWithinScheduleAndLeavesRemainderOut() {
        LocalDate date = LocalDate.of(2026, 11, 16); // Monday
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(7L);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(10, 10));
        schedule.setSlotDurationMinutes(30);
        when(scheduleRepository.findActiveSchedulesForDoctorForUpdate(4L)).thenReturn(List.of(schedule));
        when(timeSlotRepository.findByDoctorIdAndStartTimeRangeForUpdate(eq(4L), any(), any())).thenReturn(List.of());
        when(exceptionRepository.findByDoctorIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(4L, date, date))
                .thenReturn(List.of());

        service.generateSlotsForDoctor(4L, date);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TimeSlot>> slotsCaptor = ArgumentCaptor.forClass(List.class);
        verify(timeSlotRepository).saveAll(slotsCaptor.capture());
        List<TimeSlot> slots = slotsCaptor.getValue();
        assertEquals(2, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.get(0).getStartTime().toLocalTime());
        assertEquals(LocalTime.of(9, 30), slots.get(1).getStartTime().toLocalTime());
        assertTrue(slots.stream().allMatch(slot -> slot.getStatus() == TimeSlot.SlotStatus.FREE));
    }

    @Test
    void doesNotGenerateSlotsWhenDoctorIsOnException() {
        LocalDate date = LocalDate.of(2026, 11, 16);
        AvailabilityException exception = new AvailabilityException();
        when(scheduleRepository.findActiveSchedulesForDoctorForUpdate(4L)).thenReturn(List.of());
        when(timeSlotRepository.findByDoctorIdAndStartTimeRangeForUpdate(eq(4L), any(), any())).thenReturn(List.of());
        when(exceptionRepository.findByDoctorIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(4L, date, date))
                .thenReturn(List.of(exception));

        service.generateSlotsForDoctor(4L, date);

        verify(timeSlotRepository).saveAll(List.of());
    }
}
