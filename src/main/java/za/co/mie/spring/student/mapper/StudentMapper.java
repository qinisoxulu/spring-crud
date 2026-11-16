package za.co.mie.spring.student.mapper;

import za.co.mie.spring.student.dto.StudentRequest;
import za.co.mie.spring.student.dto.StudentResponse;
import za.co.mie.spring.student.entity.StudentEntity;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public StudentEntity toEntity(StudentRequest request) {
        StudentEntity student = new StudentEntity();
        updateEntity(student, request);
        return student;
    }

    public void updateEntity(StudentEntity student, StudentRequest request) {
        student.setFirstName(request.first_name());
        student.setLastName(request.last_name());
        student.setEmail(request.email());
    }

    public StudentResponse toResponse(StudentEntity student) {
        return new StudentResponse(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail()
        );
    }
}