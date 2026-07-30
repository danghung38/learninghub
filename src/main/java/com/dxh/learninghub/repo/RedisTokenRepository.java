package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.RedisToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisTokenRepository extends CrudRepository<RedisToken, String> {
}