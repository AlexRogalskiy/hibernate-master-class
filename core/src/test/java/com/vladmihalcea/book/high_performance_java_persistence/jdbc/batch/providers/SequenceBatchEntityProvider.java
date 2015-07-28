package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.providers;

import com.vladmihalcea.hibernate.masterclass.laboratory.util.EntityProvider;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <code>SequenceBatchEntityProvider</code> - SEQUENCE Batch Entity Provider
 *
 * @author Vlad Mihalcea
 */
public class SequenceBatchEntityProvider implements EntityProvider {

    @Override
    public Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
        };
    }

    @Entity(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_seq")
        @SequenceGenerator(name="post_seq", sequenceName="post_seq")
        private Long id;

        private String title;

        @Version
        private int version;

        private Post() {
        }

        public Post(String title) {
            this.title = title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
