package com.example.antipatterns.query_problems.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentDao extends JpaRepository<Comment, Long>
{
    String QUERY = "SELECT c FROM Comment c";

    @Query("SELECT c FROM Comment c")
    List<Comment> findAllComments();

    @Query("SELECT c FROM Comment c join fetch c.post")
    List<Comment> findAllCommentsUsingFetchJoin();

    @Query(QUERY)
    List<Comment> findAllCommentsWithConstant();
}
