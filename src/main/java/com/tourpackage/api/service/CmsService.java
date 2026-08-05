package com.tourpackage.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.config.CacheConfig;
import com.tourpackage.api.dto.request.BannerRequest;
import com.tourpackage.api.dto.request.BlogPostRequest;
import com.tourpackage.api.dto.request.ContentBlockRequest;
import com.tourpackage.api.dto.request.GalleryImageRequest;
import com.tourpackage.api.dto.request.NavLinkRequest;
import com.tourpackage.api.dto.request.PageSeoRequest;
import com.tourpackage.api.dto.response.BannerAdminResponse;
import com.tourpackage.api.dto.response.BlogPostAdminResponse;
import com.tourpackage.api.dto.response.BlogPostDetailResponse;
import com.tourpackage.api.dto.response.BlogPostSummaryResponse;
import com.tourpackage.api.dto.response.ContentBlockResponse;
import com.tourpackage.api.dto.response.GalleryImageResponse;
import com.tourpackage.api.dto.response.NavLinkResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.dto.response.PageSeoResponse;
import com.tourpackage.api.dto.response.SiteContentResponse;
import com.tourpackage.api.entity.Banner;
import com.tourpackage.api.entity.BannerPlacement;
import com.tourpackage.api.entity.BlogPost;
import com.tourpackage.api.entity.ContentBlock;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.GalleryImage;
import com.tourpackage.api.entity.NavGroup;
import com.tourpackage.api.entity.NavLink;
import com.tourpackage.api.entity.PageSeo;
import com.tourpackage.api.exception.ApiException;
import com.tourpackage.api.exception.DuplicateResourceException;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.repository.AdminRepository;
import com.tourpackage.api.repository.BannerRepository;
import com.tourpackage.api.repository.BlogPostRepository;
import com.tourpackage.api.repository.ContentBlockRepository;
import com.tourpackage.api.repository.GalleryImageRepository;
import com.tourpackage.api.repository.NavLinkRepository;
import com.tourpackage.api.repository.PageSeoRepository;

/**
 * The CMS: everything an admin edits that the public site then renders.
 * Content blocks and nav links are read on every page load, so they're served
 * together by {@link #getSiteContent()} rather than one request each.
 */
@Service
@Transactional
public class CmsService {

    private final ContentBlockRepository contentBlockRepository;
    private final NavLinkRepository navLinkRepository;
    private final PageSeoRepository pageSeoRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final BannerRepository bannerRepository;
    private final BlogPostRepository blogPostRepository;
    private final AdminRepository adminRepository;

    public CmsService(
            ContentBlockRepository contentBlockRepository,
            NavLinkRepository navLinkRepository,
            PageSeoRepository pageSeoRepository,
            GalleryImageRepository galleryImageRepository,
            BannerRepository bannerRepository,
            BlogPostRepository blogPostRepository,
            AdminRepository adminRepository) {
        this.contentBlockRepository = contentBlockRepository;
        this.navLinkRepository = navLinkRepository;
        this.pageSeoRepository = pageSeoRepository;
        this.galleryImageRepository = galleryImageRepository;
        this.bannerRepository = bannerRepository;
        this.blogPostRepository = blogPostRepository;
        this.adminRepository = adminRepository;
    }

    // --- public reads ----------------------------------------------------

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.SITE_CONTENT)
    public SiteContentResponse getSiteContent() {
        Map<String, ContentBlockResponse> blocks = contentBlockRepository.findByActiveTrue().stream()
                .collect(Collectors.toMap(ContentBlock::getKey, CmsService::toBlockResponse));

        List<NavLink> links = navLinkRepository.findByActiveTrueOrderByNavGroupAscDisplayOrderAsc();

        return new SiteContentResponse(
                blocks,
                links.stream().filter(l -> l.getNavGroup() == NavGroup.HEADER).map(CmsService::toNavResponse).toList(),
                links.stream().filter(l -> l.getNavGroup() == NavGroup.FOOTER).map(CmsService::toNavResponse).toList());
    }

    /**
     * SEO for one route. Returns null rather than throwing when a path has no
     * row — a page without managed metadata should fall back to its own
     * defaults, not fail to render.
     */
    // Every server-rendered page calls this once through generateMetadata, so
    // it is one of the hottest reads on the site. Nulls are cached too — see
    // CacheConfig; a route with no managed row would otherwise query on every
    // single render to learn nothing again.
    @Cacheable(value = CacheConfig.PAGE_SEO, key = "#path")
    @Transactional(readOnly = true)
    public PageSeoResponse getSeoForPath(String path) {
        return pageSeoRepository.findByPath(path).map(CmsService::toSeoResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<GalleryImageResponse> getGallery() {
        return galleryImageRepository.findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(CmsService::toGalleryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BannerAdminResponse> getBannersByPlacement(BannerPlacement placement) {
        return bannerRepository.findActiveBanners(placement, Instant.now()).stream()
                .map(CmsService::toBannerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BlogPostSummaryResponse> listPublishedPosts(String category, Pageable pageable) {
        return PageResponse.of(blogPostRepository.findPublished(category, pageable));
    }

    @Transactional(readOnly = true)
    public BlogPostDetailResponse getPostBySlug(String slug) {
        BlogPost post = blogPostRepository.findBySlugAndStatus(slug, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + slug));

        return new BlogPostDetailResponse(
                post.getId(), post.getTitle(), post.getSlug(), post.getExcerpt(), post.getContent(),
                post.getCoverImageUrl(), post.getCategory(), post.getPublishedAt(),
                post.getReadTimeMinutes(), authorName(post.getAuthorId()));
    }

    // --- content blocks --------------------------------------------------

    @Transactional(readOnly = true)
    public List<ContentBlockResponse> listBlocks() {
        return contentBlockRepository.findAll().stream()
                .sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()))
                .map(CmsService::toBlockResponse)
                .toList();
    }

    @CacheEvict(value = {CacheConfig.SITE_CONTENT, CacheConfig.PAGE_SEO}, allEntries = true)
    public ContentBlockResponse saveBlock(UUID id, ContentBlockRequest request) {
        boolean duplicate = id == null
                ? contentBlockRepository.existsByKey(request.key())
                : contentBlockRepository.existsByKeyAndIdNot(request.key(), id);
        if (duplicate) {
            throw new DuplicateResourceException("A content block with key '" + request.key() + "' already exists");
        }

        Instant now = Instant.now();
        ContentBlock block = id == null
                ? ContentBlock.builder().createdAt(now).build()
                : contentBlockRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Content block not found: " + id));

        block.setKey(request.key().trim());
        block.setEyebrow(blankToNull(request.eyebrow()));
        block.setTitle(blankToNull(request.title()));
        block.setSubtitle(blankToNull(request.subtitle()));
        block.setBody(blankToNull(request.body()));
        block.setActive(request.isActive());
        block.setUpdatedAt(now);

        return toBlockResponse(contentBlockRepository.save(block));
    }

    @CacheEvict(value = {CacheConfig.SITE_CONTENT, CacheConfig.PAGE_SEO}, allEntries = true)
    public void deleteBlock(UUID id) {
        contentBlockRepository.delete(contentBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content block not found: " + id)));
    }

    // --- page SEO --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PageSeoResponse> listSeo() {
        return pageSeoRepository.findAllByOrderByPathAsc().stream().map(CmsService::toSeoResponse).toList();
    }

    @CacheEvict(value = {CacheConfig.SITE_CONTENT, CacheConfig.PAGE_SEO}, allEntries = true)
    public PageSeoResponse saveSeo(UUID id, PageSeoRequest request) {
        boolean duplicate = id == null
                ? pageSeoRepository.existsByPath(request.path())
                : pageSeoRepository.existsByPathAndIdNot(request.path(), id);
        if (duplicate) {
            throw new DuplicateResourceException("SEO for path '" + request.path() + "' already exists");
        }

        Instant now = Instant.now();
        PageSeo seo = id == null
                ? PageSeo.builder().createdAt(now).build()
                : pageSeoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Page SEO not found: " + id));

        seo.setPath(request.path().trim());
        seo.setMetaTitle(request.metaTitle().trim());
        seo.setMetaDescription(blankToNull(request.metaDescription()));
        seo.setOgImageUrl(blankToNull(request.ogImageUrl()));
        seo.setNoIndex(request.noIndex());
        seo.setUpdatedAt(now);

        return toSeoResponse(pageSeoRepository.save(seo));
    }

    @CacheEvict(value = {CacheConfig.SITE_CONTENT, CacheConfig.PAGE_SEO}, allEntries = true)
    public void deleteSeo(UUID id) {
        pageSeoRepository.delete(pageSeoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Page SEO not found: " + id)));
    }

    // --- nav links -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NavLinkResponse> listNavLinks() {
        return navLinkRepository.findAllByOrderByNavGroupAscDisplayOrderAsc().stream()
                .map(CmsService::toNavResponse)
                .toList();
    }

    @CacheEvict(value = {CacheConfig.SITE_CONTENT, CacheConfig.PAGE_SEO}, allEntries = true)
    public NavLinkResponse saveNavLink(UUID id, NavLinkRequest request) {
        Instant now = Instant.now();
        NavLink link = id == null
                ? NavLink.builder().createdAt(now).build()
                : navLinkRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Nav link not found: " + id));

        link.setNavGroup(request.navGroup());
        link.setLabel(request.label().trim());
        link.setHref(request.href().trim());
        link.setDisplayOrder(request.displayOrder());
        link.setActive(request.isActive());
        link.setUpdatedAt(now);

        return toNavResponse(navLinkRepository.save(link));
    }

    @CacheEvict(value = {CacheConfig.SITE_CONTENT, CacheConfig.PAGE_SEO}, allEntries = true)
    public void deleteNavLink(UUID id) {
        NavLink link = navLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nav link not found: " + id));

        // Removing every header link would leave the site with no navigation
        // and no way back to this screen except by typing a URL.
        if (link.getNavGroup() == NavGroup.HEADER
                && navLinkRepository.findByNavGroupAndActiveTrueOrderByDisplayOrderAsc(NavGroup.HEADER).size() <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "At least one header link must remain");
        }

        navLinkRepository.delete(link);
    }

    // --- gallery ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<GalleryImageResponse> listGalleryImages() {
        return galleryImageRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(CmsService::toGalleryResponse)
                .toList();
    }

    public GalleryImageResponse saveGalleryImage(UUID id, GalleryImageRequest request) {
        Instant now = Instant.now();
        GalleryImage image = id == null
                ? GalleryImage.builder().createdAt(now).build()
                : galleryImageRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Gallery image not found: " + id));

        image.setUrl(request.url().trim());
        image.setAltText(blankToNull(request.altText()));
        image.setCaption(blankToNull(request.caption()));
        image.setCategory(request.category().trim());
        image.setDisplayOrder(request.displayOrder());
        image.setActive(request.isActive());
        image.setUpdatedAt(now);

        return toGalleryResponse(galleryImageRepository.save(image));
    }

    public void deleteGalleryImage(UUID id) {
        galleryImageRepository.delete(galleryImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery image not found: " + id)));
    }

    /**
     * Applies a new order from a list of ids — position in the list becomes
     * {@code display_order}.
     *
     * <p>Ids not in the list keep their existing order value, so a partial
     * list (one page of a paginated grid) reorders only what it was showing
     * rather than pushing everything else to zero. Unknown ids are rejected
     * rather than ignored: silently dropping one would leave the client
     * believing an order it isn't going to see.
     */
    public List<GalleryImageResponse> reorderGallery(List<UUID> orderedIds) {
        List<GalleryImage> images = galleryImageRepository.findAllById(orderedIds);

        if (images.size() != orderedIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more images could not be found");
        }

        Instant now = Instant.now();
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            int position = i;
            images.stream()
                    .filter(image -> image.getId().equals(id))
                    .findFirst()
                    .ifPresent(image -> {
                        image.setDisplayOrder(position);
                        image.setUpdatedAt(now);
                    });
        }

        galleryImageRepository.saveAll(images);
        return listGalleryImages();
    }

    // --- banners ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BannerAdminResponse> listBanners() {
        return bannerRepository.findAllByOrderByPlacementAscDisplayOrderAsc().stream()
                .map(CmsService::toBannerResponse)
                .toList();
    }

    public BannerAdminResponse saveBanner(UUID id, BannerRequest request) {
        if (request.startsAt() != null && request.endsAt() != null
                && !request.endsAt().isAfter(request.startsAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End date must be after the start date");
        }

        Instant now = Instant.now();
        Banner banner = id == null
                ? Banner.builder().createdAt(now).build()
                : bannerRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Banner not found: " + id));

        banner.setPlacement(request.placement());
        banner.setTitle(blankToNull(request.title()));
        banner.setSubtitle(blankToNull(request.subtitle()));
        banner.setImageUrl(request.imageUrl().trim());
        banner.setLinkUrl(blankToNull(request.linkUrl()));
        banner.setButtonLabel(blankToNull(request.buttonLabel()));
        banner.setDisplayOrder(request.displayOrder());
        banner.setActive(request.isActive());
        banner.setStartsAt(request.startsAt());
        banner.setEndsAt(request.endsAt());
        banner.setUpdatedAt(now);

        return toBannerResponse(bannerRepository.save(banner));
    }

    public void deleteBanner(UUID id) {
        bannerRepository.delete(bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found: " + id)));
    }

    // --- blog posts ------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BlogPostAdminResponse> listPosts() {
        return blogPostRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toPostAdminResponse)
                .toList();
    }

    public BlogPostAdminResponse savePost(UUID id, BlogPostRequest request, UUID authorId) {
        boolean duplicate = id == null
                ? blogPostRepository.existsBySlug(request.slug())
                : blogPostRepository.existsBySlugAndIdNot(request.slug(), id);
        if (duplicate) {
            throw new DuplicateResourceException("A post with slug '" + request.slug() + "' already exists");
        }

        Instant now = Instant.now();
        BlogPost post = id == null
                ? BlogPost.builder().createdAt(now).authorId(authorId).build()
                : blogPostRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + id));

        post.setTitle(request.title().trim());
        post.setSlug(request.slug().trim());
        post.setExcerpt(blankToNull(request.excerpt()));
        post.setContent(request.content().trim());
        post.setCoverImageUrl(blankToNull(request.coverImageUrl()));
        post.setCategory(request.category().trim());
        post.setStatus(request.status());
        post.setReadTimeMinutes(request.readTimeMinutes());
        post.setUpdatedAt(now);

        // ck_blog_posts_published_at requires published_at exactly when the
        // status is PUBLISHED, so both directions are handled: stamp it on the
        // first publish, keep it on re-publish, clear it when unpublished.
        if (request.status() == ContentStatus.PUBLISHED) {
            if (post.getPublishedAt() == null) {
                post.setPublishedAt(now);
            }
        } else {
            post.setPublishedAt(null);
        }

        return toPostAdminResponse(blogPostRepository.save(post));
    }

    public void deletePost(UUID id) {
        blogPostRepository.delete(blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + id)));
    }

    // --- mapping ---------------------------------------------------------

    private String authorName(UUID authorId) {
        return authorId == null ? null
                : adminRepository.findById(authorId).map(a -> a.getFullName()).orElse(null);
    }

    private BlogPostAdminResponse toPostAdminResponse(BlogPost p) {
        return new BlogPostAdminResponse(
                p.getId(), p.getTitle(), p.getSlug(), p.getExcerpt(), p.getContent(),
                p.getCoverImageUrl(), p.getCategory(), p.getStatus(), p.getPublishedAt(),
                p.getReadTimeMinutes(), authorName(p.getAuthorId()), p.getCreatedAt());
    }

    private static ContentBlockResponse toBlockResponse(ContentBlock b) {
        return new ContentBlockResponse(
                b.getId(), b.getKey(), b.getEyebrow(), b.getTitle(), b.getSubtitle(), b.getBody(), b.isActive());
    }

    private static PageSeoResponse toSeoResponse(PageSeo s) {
        return new PageSeoResponse(
                s.getId(), s.getPath(), s.getMetaTitle(), s.getMetaDescription(),
                s.getOgImageUrl(), s.isNoIndex());
    }

    private static NavLinkResponse toNavResponse(NavLink l) {
        return new NavLinkResponse(
                l.getId(), l.getNavGroup(), l.getLabel(), l.getHref(), l.getDisplayOrder(), l.isActive());
    }

    private static GalleryImageResponse toGalleryResponse(GalleryImage g) {
        return new GalleryImageResponse(
                g.getId(), g.getUrl(), g.getAltText(), g.getCaption(),
                g.getCategory(), g.getDisplayOrder(), g.isActive());
    }

    private static BannerAdminResponse toBannerResponse(Banner b) {
        return new BannerAdminResponse(
                b.getId(), b.getPlacement(), b.getTitle(), b.getSubtitle(), b.getImageUrl(),
                b.getLinkUrl(), b.getButtonLabel(), b.getDisplayOrder(), b.isActive(),
                b.getStartsAt(), b.getEndsAt());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
