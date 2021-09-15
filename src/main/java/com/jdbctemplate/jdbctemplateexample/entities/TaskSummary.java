package com.jdbctemplate.jdbctemplateexample.entities;

public class TaskSummary {

    private String taskId;

    public TaskSummary(String taskName) {
        this.taskId = taskName;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
