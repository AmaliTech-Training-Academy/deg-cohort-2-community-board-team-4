package com.amalitech.communityboard.repository;

import com.amalitech.communityboard.model.Post;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class PostSpecifications {

    private PostSpecifications() {
    }


    public static Specification<Post> hasCategory(String categoryName) {
        if (categoryName == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("category").get("name")), categoryName.toLowerCase());
    }

    public static Specification<Post> matchesKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("content")), pattern));
    }

    public static Specification<Post> createdFrom(LocalDateTime from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }


    public static Specification<Post> createdTo(LocalDateTime to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
