package api.giybat.uz.api.giybat.uz.repository;

import api.giybat.uz.api.giybat.uz.dto.FilterResultDTO;
import api.giybat.uz.api.giybat.uz.dto.post.PostAdminFilterDTO;
import api.giybat.uz.api.giybat.uz.dto.post.PostFilterDTO;
import api.giybat.uz.api.giybat.uz.entity.PostEntity;
import api.giybat.uz.api.giybat.uz.service.mapper.PostDetailMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CustomPostRepository {

    @Autowired
    private EntityManager entityManager;

    public FilterResultDTO<PostEntity> filter(PostFilterDTO filter, int page, int size){

        StringBuilder queryBuilder = new StringBuilder("where p.visible = true ");
        Map<String, Object> params = new HashMap<>();

        // agar filter kelsa ushbu shart ishlaydi, agar kelmasa keyingiga o'tib ketaveradi
        if (filter.getQuery() != null){
            queryBuilder.append("and lower(p.title) like :query ");
            params.put("query", "%" + filter.getQuery().toLowerCase() + "%");
        }
        // get similar uchun filter
        if (filter.getExceptId() != null){
            queryBuilder.append("and p.id != :exceptId ");
            params.put("exceptId", filter.getExceptId());
        }

        // oxirgi yaratilgan post birinchi listda ko'rinadi
//        queryBuilder.append("order by p.createdDate desc");

        StringBuilder selectBuilder = new StringBuilder("Select p From PostEntity p ")
                .append(queryBuilder)
                .append("order by p.createdDate desc");
        StringBuilder countBuilder = new StringBuilder("Select count(p) From PostEntity p ")
                .append(queryBuilder);

        // select
        Query selectQuery = entityManager.createQuery(selectBuilder.toString());
        selectQuery.setFirstResult((page) * size); // ofset - 50
        selectQuery.setMaxResults(size); // limit - 30

        for(Map.Entry<String, Object> entry: params.entrySet()){
            selectQuery.setParameter(entry.getKey(),entry.getValue());
        }

        List<PostEntity> entityList = selectQuery.getResultList();
        // count
        Query countQuery = entityManager.createQuery(countBuilder.toString());
        for(Map.Entry<String, Object> entry: params.entrySet()){
            countQuery.setParameter(entry.getKey(),entry.getValue());
        }
        Long totalCount = (Long) countQuery.getSingleResult();

        return new FilterResultDTO<>(entityList,totalCount);

    }

    public FilterResultDTO<Object[]> filter(PostAdminFilterDTO filter, int page, int size){

        StringBuilder queryBuilder = new StringBuilder("where p.visible = true ");
        Map<String, Object> params = new HashMap<>();

        // agar filter kelsa ushbu shart ishlaydi, agar kelmasa keyingiga o'tib ketaveradi
        if (filter.getProfileQuery() != null && !filter.getProfileQuery().isBlank()){
            queryBuilder.append("and (lower(pr.name) like :profileQuery or lower(pr.username) like :profileQuery) ");
            params.put("profileQuery", "%" + filter.getProfileQuery().toLowerCase() + "%");
        }
        // get similar uchun filter
        if (filter.getPostQuery() != null && !filter.getPostQuery().isBlank()){
            queryBuilder.append("and (lower(p.title) like :postQuery or p.id = :postId) ");
            params.put("postQuery", "%" + filter.getPostQuery().toLowerCase() + "%");
            params.put("postId", filter.getPostQuery().toLowerCase());
        }

        // oxirgi yaratilgan post birinchi listda ko'rinadi
//        queryBuilder.append("order by p.createdDate desc");

        StringBuilder selectBuilder = new StringBuilder("Select p.id as postId, p.title as postTitle, p.photoId as postPhotoId, p.createdDate as postCreatdeDate, " +
                "pr.id as profileId, pr.name as profileName, pr.username as profileUsername from PostEntity p")
                .append(" inner join p.profile as pr ")
                .append(queryBuilder)
                .append("order by p.createdDate desc ");
        StringBuilder countBuilder = new StringBuilder("Select count(p) From PostEntity p inner join p.profile as pr ")
                .append(queryBuilder);

        // select
        Query selectQuery = entityManager.createQuery(selectBuilder.toString());
        selectQuery.setFirstResult((page) * size); // ofset - 50
        selectQuery.setMaxResults(size); // limit - 30

        for(Map.Entry<String, Object> entry: params.entrySet()){
            selectQuery.setParameter(entry.getKey(),entry.getValue());
        }

        List<Object[]> entityList = selectQuery.getResultList();
        // count
        Query countQuery = entityManager.createQuery(countBuilder.toString());
        for(Map.Entry<String, Object> entry: params.entrySet()){
            countQuery.setParameter(entry.getKey(),entry.getValue());
        }
        Long totalCount = (Long) countQuery.getSingleResult();

        return new FilterResultDTO<>(entityList,totalCount);

    }
}
