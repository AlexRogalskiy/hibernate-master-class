package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.providers;

import com.vladmihalcea.hibernate.masterclass.laboratory.util.EntityProvider;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <code>BatchEntityProvider</code> - Batch Entity Provider
 *
 * @author Vlad Mihalcea
 */
public class BatchEntityProvider implements EntityProvider {

    @Override
    public Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostDetails.class,
                Comment.class
        };
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

        private Post() {
        }

        public Post(String title) {
            this.title = title;
        }

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        private List<Comment> comments = new ArrayList<>();

        @OneToOne(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true, fetch = FetchType.LAZY)
        private PostDetails details;

        public void setTitle(String title) {
            this.title = title;
        }

        public List<Comment> getComments() {
            return comments;
        }

        public PostDetails getDetails() {
            return details;
        }

        public void addComment(Comment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        public void addDetails(PostDetails details) {
            this.details = details;
            details.setPost(this);
        }

        public void removeDetails() {
            this.details.setPost(null);
            this.details = null;
        }
    }

    @Entity(name = "PostDetails")
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Version
        private int version;

        public PostDetails() {
            createdOn = new Date();
        }

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "id")
        @MapsId
        private Post post;

        public Long getId() {
            return id;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "PostComment", indexes = @Index(columnList = "post_id", name = "POST_ID_FK_IDX"))
    public static class Comment {

        @Id
        private Long id;

        @ManyToOne
        private Post post;

        @Version
        private int version;

        private Comment() {
        }

        public Comment(String review) {
            this.review = review;
        }

        private String review;

        public Long getId() {
            return id;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }
}
