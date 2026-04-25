package api.giybat.uz.api.giybat.uz.repository;

import api.giybat.uz.api.giybat.uz.entity.PostEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends CrudRepository<PostEntity, String>, PagingAndSortingRepository<PostEntity,String> {

//    @Query("select * from post where profile_id = ? and visible = true")
    Page<PostEntity> getAllByProfileIdAndVisibleTrueOrderByCreatedDateDesc(Integer profileId, Pageable pageable);

    @Query("from PostEntity where id != ?1 and visible = true order by createdDate desc limit 3")
    List<PostEntity> getSimilarPostList(String exceptId);
    @Transactional
    @Modifying
    @Query("update PostEntity set visible = false where id = ?1")
    void delete(String id);
}
