package api.giybat.uz.api.giybat.uz.service;

import api.giybat.uz.api.giybat.uz.dto.AppResponse;
import api.giybat.uz.api.giybat.uz.dto.FilterResultDTO;
import api.giybat.uz.api.giybat.uz.dto.ProfileDTO;
import api.giybat.uz.api.giybat.uz.dto.post.*;
import api.giybat.uz.api.giybat.uz.entity.PostEntity;
import api.giybat.uz.api.giybat.uz.enums.GeneralStatus;
import api.giybat.uz.api.giybat.uz.enums.ProfileRole;
import api.giybat.uz.api.giybat.uz.exps.AppBadException;
import api.giybat.uz.api.giybat.uz.repository.CustomPostRepository;
import api.giybat.uz.api.giybat.uz.repository.PostRepository;
import api.giybat.uz.api.giybat.uz.service.mapper.PostDetailMapper;
import api.giybat.uz.api.giybat.uz.util.SpringSecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AttachService attachService;
    @Autowired
    private CustomPostRepository customPostRepository;

    public PostDTO create(PostCreateDTO dto){
        PostEntity entity = new PostEntity();
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setPhotoId(dto.getPhoto().getId());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setVisible(true);
        entity.setStatus(GeneralStatus.BLOCK);
        entity.setProfileId(SpringSecurityUtil.getCurrentUserId());
        postRepository.save(entity);

        return toInfoDto(entity);
    }

    public Page<PostDTO> getProfilePostList(int page, int size){
        PageRequest pageRequest = PageRequest.of(page,size);
        Integer profId = SpringSecurityUtil.getCurrentUserId();
        Page<PostEntity> result = postRepository.getAllByProfileIdAndVisibleTrueOrderByCreatedDateDesc(profId, pageRequest);
        List<PostDTO> dtoList = result.getContent().stream()
                .map(dto -> toInfoDto(dto))
                .toList();

        return new PageImpl<PostDTO>(dtoList, pageRequest, result.getTotalElements());
    }

    public PostDTO getById(String id){
        PostEntity entity = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        return toDto(entity);
    }

    public PostDTO update(String id, PostCreateDTO dto){
        PostEntity entity = get(id);
        if (!SpringSecurityUtil.hazRole(ProfileRole.ROLE_ADMIN) && !entity.getProfileId().equals(SpringSecurityUtil.getCurrentUserId())){
            throw new AppBadException("You do not have permission to update this post");
        }
        String deletePhotoId = null;
        if (!dto.getPhoto().getId().equals(entity.getPhotoId())){
            deletePhotoId = entity.getPhotoId();
        }
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setPhotoId(dto.getPhoto().getId());
        postRepository.save(entity);
        // delete old image
        if(deletePhotoId != null){
            attachService.delete(deletePhotoId);
        }
        return toInfoDto(entity);
    }

    public AppResponse<String> delete(String id){
        PostEntity entity = get(id);
        if (!SpringSecurityUtil.hazRole(ProfileRole.ROLE_ADMIN) && !entity.getProfileId().equals(SpringSecurityUtil.getCurrentUserId())){
            throw new AppBadException("You do not have permission to update this post");
        }
        postRepository.delete(id);
        return new AppResponse<>("Post muvoffaqiyatli o'chirildi.");
    }

    public PageImpl<PostDTO> filter(PostFilterDTO dto, int page, int size){
        FilterResultDTO<PostEntity> resultDto = customPostRepository.filter(dto, page, size);
        List<PostDTO> dtoList = resultDto.getList().stream()
                .map(postEntity -> toInfoDto(postEntity))
                .toList();
        return new PageImpl<>(dtoList, PageRequest.of(page,size), resultDto.getTotalCount());
    }

    public PageImpl<PostDTO> adminFilter(PostAdminFilterDTO dto, int page, int size) {
        FilterResultDTO<Object[]> resultDto = customPostRepository.filter(dto, page, size);
        List<PostDTO> dtoList = resultDto.getList().stream()
                .map(postEntity -> toDto(postEntity))
                .toList();
        return new PageImpl<>(dtoList, PageRequest.of(page,size), resultDto.getTotalCount());
    }

    public List<PostDTO> getSimilarPostList(SimilarPostListDTO dto) {
        log.info("ssssssssiiiimmmmmmmmm---", dto.getExceptId());
        List<PostEntity> postEntitiesList = postRepository.getSimilarPostList(dto.getExceptId());

        List<PostDTO> dtoList = postEntitiesList.stream()
                .map(this::toInfoDto)
                .toList();
        return dtoList;
    }
    public PostDTO toDto(PostEntity entity){
        PostDTO dto = new PostDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setPhoto(attachService.attachDTO(entity.getPhotoId()));
        return dto;
    }

    public PostDTO toDto(Object[] obj){
        PostDTO post = new PostDTO();
        post.setId((String) obj[0]);
        post.setTitle((String) obj[1]);
        if (obj[2] != null){
            post.setPhoto(attachService.attachDTO((String) obj[2]));
        }
        post.setCreatedDate((LocalDateTime) obj[3]);

        ProfileDTO profile = new ProfileDTO();
        profile.setId((Integer) obj[4]);
        profile.setName((String) obj[5]);
        profile.setUsername((String) obj[6]);

        post.setProfile(profile);
        return post;
    }

    public PostDTO toInfoDto(PostEntity entity){
        PostDTO dto = new PostDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setPhoto(attachService.attachDTO(entity.getPhotoId()));
        return dto;
    }

    public PostEntity get(String id){
        return postRepository.findById(id).orElseThrow(() -> {
            throw new AppBadException("Post not found: " + id);
        });
    }


}
