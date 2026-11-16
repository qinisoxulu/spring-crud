package za.co.mie.spring.student.dto;

public record StudentResponse(
        Long id,
        String firstName,
        String lastName,
        String email
) { }