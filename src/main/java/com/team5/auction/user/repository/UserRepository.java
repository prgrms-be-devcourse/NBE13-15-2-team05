package com.team5.auction.user.repository;

import com.team5.auction.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	Optional<User> findByNickname(String nickname);

	// 회원 정보 수정(C2) 전용. 쓰기 락을 걸어 포인트 수정에 대한 덮어씌워짐을 방지
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> findByIdForUpdate(@Param("id") Long id);

	/**
	 * PointService 전용. 유저 행에 쓰기 락을 id 오름차순으로 겁니다. (계약서 2.2)
	 * 다른 서비스에서는 호출하지 않습니다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id in :ids order by u.id asc")
	List<User> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);
}
