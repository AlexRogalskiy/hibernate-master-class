package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.providers;

import com.vladmihalcea.hibernate.masterclass.laboratory.util.EntityProvider;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <code>TaskEntityProvider</code> - Task Entity Provider
 *
 * @author Vlad Mihalcea
 */
public class TaskEntityProvider implements EntityProvider {

    public enum StatusType {
        TO_D0,
        DONE,
        FAILED
    }

    @Override
    public Class<?>[] entities() {
        return new Class<?>[]{
                Task.class
        };
    }

    @Entity(name = "task")
    public static class Task {

        @Id
        private Long id;

        @Enumerated(EnumType.STRING)
        private StatusType status;
    }
}
