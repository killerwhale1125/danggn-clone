package carrot.market.post.service;

import carrot.market.common.baseutil.BaseException;
import carrot.market.member.entity.Member;
import carrot.market.member.repository.MemberRepository;
import carrot.market.post.dto.PostRequestDto;
import carrot.market.post.entity.Category;
import carrot.market.post.entity.Post;
import carrot.market.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static carrot.market.common.baseutil.BaseResponseStatus.*;
import static carrot.market.config.CacheKeyConfig.POST;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final CategoryService categoryService;

    public Post findPostById(Long postId) {
        return postRepository.findPostById(postId).orElseThrow(() -> new BaseException(NOT_EXISTED_POST));
    }

    /**
     * 게시물 생성
     * 게시물 생성 시 카테고리는 필수적으로 입력해야하기 때문에 캐싱 적용
     */
    @Transactional
    public void createNewPost(PostRequestDto postRequest, String email) {
        Member member = memberRepository.findMemberByEmail(email).orElseThrow(() -> new BaseException(NOT_EXISTED_USER));

        Post post = postRequest.toEntity(member);
        /** 카테고리 캐시 조회 및 저장 **/
        Category category = categoryService.findCategoryByName(postRequest.getCategory());

        post.addCategory(category);

        postRepository.save(post);
    }

    /**
     * 게시물 업데이트
     * @CacheEvict -> 캐시 데이터 제거
     */
    @Caching(evict = {
            @CacheEvict(
                    key = "#member.getAddress().state + '.' + #member.getAddress().city + '.' + #member.getAddress().town",
                    value = POST
            ),
            @CacheEvict(
                    key = "#member.getAddress().state + '.' + #member.getAddress().city + '.' + #member.getAddress().town + '.' + #postRequest.category",
                    value = POST
            )
    })
    public void updatePost(Post post, PostRequestDto postRequest, Member member) {
        if(checkAuth(post, member)) {
            Category category = categoryService.findCategoryByName(postRequest.getCategory());

            post.updatePost(postRequest);
            post.setCategory(category);
        }
    }

    @Caching(evict = {
            @CacheEvict(
                    key = "#member.getAddress().state + '.' + #member.getAddress().city + '.' + #member.getAddress().town",
                    value = POST
            ),
            @CacheEvict(
                    key = "#member.getAddress().state + '.' + #member.getAddress().city + '.' + #member.getAddress().town + '.' + #postRequest.category",
                    value = POST
            )
    })
    public void removePost(Post post, Member member) {
        if (checkAuth(post, member)) {
            post.removePost();
        }
    }

    private boolean checkAuth(Post post, Member member) {
        if(post.getAuthor().getId() != member.getId())
            throw new BaseException(AUTHORIZATION_FAIL);
        return true;
    }
}
