package za.co.mie.spring.student.repository;

import za.co.mie.spring.student.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<StudentEntity> findByEmail(String email);
}