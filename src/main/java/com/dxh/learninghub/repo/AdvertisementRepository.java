package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Advertisement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    @EntityGraph(attributePaths = "course")
    @Query("""
            select advertisement
            from Advertisement advertisement
            where (:active is null or advertisement.active = :active)
              and (:sent is null or advertisement.sent = :sent)
              and (:title is null or lower(advertisement.title) like lower(concat('%', :title, '%')))
            order by advertisement.createdAt desc
            """)
    List<Advertisement> searchAdvertisements(
            @Param("active") Boolean active,
            @Param("sent") Boolean sent,
            @Param("title") String title);

    @EntityGraph(attributePaths = "course")
    Optional<Advertisement> findWithCourseById(Long id);

    @EntityGraph(attributePaths = "course")
    @Query("""
            select advertisement
            from Advertisement advertisement
            where advertisement.active = true
              and advertisement.startDate <= CURRENT_DATE
              and advertisement.endDate >= CURRENT_DATE
              and (
                    advertisement.course is null
                    or advertisement.course.status =
                       com.dxh.learninghub.enums.CourseStatus.APPROVED
              )
            order by advertisement.createdAt desc
            """)
    List<Advertisement> findActiveAdvertisements();

    // Thêm method tìm danh sách quảng cáo đã hết hạn để lấy đường dẫn ảnh xóa trên S3
    @Query("select a from Advertisement a where a.endDate < :currentDate")
    List<Advertisement> findExpiredAdvertisements(@Param("currentDate") LocalDate currentDate);

    @Modifying
    @Query("""
            update Advertisement a
            set a.active = false
            where a.active = true
              and a.endDate < :currentDate
            """)
    int deactivateExpiredAdvertisements(@Param("currentDate") LocalDate currentDate);
}
