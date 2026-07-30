package com.dxh.learninghub.repo;


import com.dxh.learninghub.entity.RedisVerificationToken;
import com.dxh.learninghub.enums.VerifyType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RedisVerificationTokenRepository extends CrudRepository<RedisVerificationToken, String> {

    List<RedisVerificationToken> findByUserIdAndVerifyType(Long userId, VerifyType verifyType);
}