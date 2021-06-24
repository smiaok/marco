package com.fudan.intellij.plugin.marco.entity;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Data
public class MarkReview implements Serializable {

    private static final long serialVersionUID = -1770117176436016029L;

    private long markReviewId;

    private String marker;

    private String project;

    private String version;

    private String file;

    private String line;

    private String type;

    private String status;

    private String comment;

    private String branch;

    private String reviewer;

    private Date createTime;

    private Date updateTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MarkReview that = (MarkReview) o;
        return this.markReviewId == that.markReviewId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(markReviewId);
    }
}
