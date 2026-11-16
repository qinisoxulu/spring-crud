package za.co.mie.spring.student.service;

import za.co.mie.spring.student.dto.StudentRequest;
import za.co.mie.spring.student.dto.StudentResponse;
import za.co.mie.spring.student.entity.StudentEntity;
import za.co.mie.spring.student.exceptions.DuplicateResourceException;
import za.co.mie.spring.student.exceptions.ResourceNotFoundException;
import za.co.mie.spring.student.mapper.StudentMapper;
import za.co.mie.spring.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    public StudentService(StudentRepository studentRepository, StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
    }

    @Transactional
    public StudentResponse createStudent(StudentRequest request) {
        if (studentRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A student with this email already exists");
        }
        StudentEntity saved = studentRepository.save(studentMapper.toEntity(request));
        return studentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll().stream().map(studentMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        return studentMapper.toResponse(findStudentOrThrow(id));
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentRequest request) {
        StudentEntity student = findStudentOrThrow(id);

        if (studentRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("Another student is already using this email");
        }

        studentMapper.updateEntity(student, request);
        return studentMapper.toResponse(studentRepository.save(student));
    }

    @Transactional
    public void deleteStudent(Long id) {
        studentRepository.delete(findStudentOrThrow(id));
    }

    private StudentEntity findStudentOrThrow(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
    }
}