package com.team5.auction.user.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team5.auction.user.domain.User;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * PointService 전용. 유저 행에 쓰기 락을 id 오름차순으로 겁니다. (계약서 2.2)
	 * 다른 서비스에서는 호출하지 않습니다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id in :ids order by u.id asc")
	List<User> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);
}
