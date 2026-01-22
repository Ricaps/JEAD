package com.example.antipatterns.query_problems.post;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostDao extends JpaRepository<Post, Long>
{
}
