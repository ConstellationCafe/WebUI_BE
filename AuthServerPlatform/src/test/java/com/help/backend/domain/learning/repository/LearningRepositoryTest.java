//package com.help.backend.domain.learning.repository;
//
//import com.help.backend.domain.learning.entity.LearningEntity;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//class LearningRepositoryTest {
//    @Autowired
//    LearningRepository learningRepository;
//
//    @Test
//    void findByTeacherTest() {
//        // given
//        LearningEntity e1 = LearningEntity.of("수학", "math", "1234");
//        LearningEntity e2 = LearningEntity.of("영어", "english", "1234");
//        LearningEntity e3 = LearningEntity.of("과학", "lee", "4567");
//
//        learningRepository.save(e1);
//        learningRepository.save(e2);
//        learningRepository.save(e3);
//
//        // when
//        List<LearningEntity> result = learningRepository.findByTeacher("1234");
//
//        // then
//        assertThat(result).hasSize(2);
//        assertThat(result)
//                .extracting(LearningEntity::getTeacher)
//                .containsOnly("1234");
//    }
//}
