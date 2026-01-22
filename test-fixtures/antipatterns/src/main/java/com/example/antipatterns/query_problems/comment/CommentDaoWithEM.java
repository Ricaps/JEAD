package com.example.antipatterns.query_problems.comment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentDaoWithEM
{
    private static final String SELECT_COMMENT_WITH_N_PLUS_ONE_PROBLEM = "select pc from Comment pc";
    private static final String SELECT_COMMENT_WITHOUT_N_PLUS_ONE_PROBLEM = "select pc from Comment pc join fetch pc.post";
    @PersistenceContext
    private EntityManager em;

    public List<Comment> findCommentsWithNPlus1QueryProblem()
    {
        return em.createQuery("select pc from Comment pc", Comment.class)
                .getResultList();
    }

    public List<Comment> findCommentsWithoutNPlus1QueryProblem()
    {
        return em.createQuery("select pc from Comment pc join fetch pc.post", Comment.class)
                .getResultList();
    }

    public List<Comment> findCommentsWithNPlus1QueryProblemWithConstant()
    {
        return em.createQuery(SELECT_COMMENT_WITH_N_PLUS_ONE_PROBLEM, Comment.class)
                .getResultList();
    }

    public List<Comment> findCommentsWithoutNPlus1QueryProblemWithConstant()
    {
        return em.createQuery(SELECT_COMMENT_WITHOUT_N_PLUS_ONE_PROBLEM, Comment.class)
                .getResultList();
    }

    public List<Comment> findCommentsWithNPlus1QueryProblemWithVariable()
    {
        String qlQuery = "select pc from Comment pc";
        return em.createQuery(qlQuery, Comment.class).getResultList();
    }

    public List<Comment> findCommentsWithoutNPlus1QueryProblemWithVariable()
    {
        String qlQuery = "select pc from Comment pc join fetch pc.post";
        return em.createQuery(qlQuery, Comment.class).getResultList();
    }
}
