package com.team5.auction.auction.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.auction.dto.AuctionListResponse;
import com.team5.auction.category.domain.Category;
import com.team5.auction.common.config.JpaConfig;
import com.team5.auction.common.config.TimeConfig;
import com.team5.auction.user.domain.User;

import jakarta.persistence.EntityManager;

/**
 * 경매 목록 조회 쿼리(findAuctionList)를 실제 MySQL(Testcontainers)에서 확인합니다.
 * JPQL 문자열·서브쿼리·Pageable 정렬은 Mockito로는 검증할 수 없어서 DB를 띄웁니다. Docker가 필요합니다.
 *
 * <pre>
 * 테스트 데이터 (정렬 기준마다 순서가 모두 다르게 잡음)
 *  이름 | 카테고리 | 상태    | 마감     | 생성     | 현재가
 *  A    | 전자기기 | ACTIVE | +1일 후  | 5일 전   | 50,000
 *  B    | 전자기기 | ACTIVE | +7일 후  | 1일 전   | 30,000
 *  C    | 전자기기 | ACTIVE | +3일 후  | 2일 전   | 90,000
 *  D    | 전자기기 | ACTIVE | +5일 후  | 4일 전   | 10,000
 *  E    | 의류     | ACTIVE | +2일 후  | 3일 전   | 20,000
 *  F    | 의류     | FAILED | 12시간 전 | 6일 전   | 70,000
 * </pre>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, TimeConfig.class})
@Testcontainers
class AuctionRepositoryTest {

	@Container
	@ServiceConnection
	static MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 10, 0);

	@Autowired
	private AuctionRepository auctionRepository;

	@Autowired
	private EntityManager em;

	private Category electronics;
	private Category clothes;

	@BeforeEach
	void setUp() {
		User seller = User.createUser("seller@test.com", "password", "판매자", null);
		electronics = Category.create("전자기기");
		clothes = Category.create("의류");
		em.persist(seller);
		em.persist(electronics);
		em.persist(clothes);

		saveAuction(seller, electronics, "A", AuctionStatus.ACTIVE, NOW.plusDays(1), NOW.minusDays(5), 50_000);
		saveAuction(seller, electronics, "B", AuctionStatus.ACTIVE, NOW.plusDays(7), NOW.minusDays(1), 30_000);
		saveAuction(seller, electronics, "C", AuctionStatus.ACTIVE, NOW.plusDays(3), NOW.minusDays(2), 90_000);
		saveAuction(seller, electronics, "D", AuctionStatus.ACTIVE, NOW.plusDays(5), NOW.minusDays(4), 10_000);
		saveAuction(seller, clothes, "E", AuctionStatus.ACTIVE, NOW.plusDays(2), NOW.minusDays(3), 20_000);
		saveAuction(seller, clothes, "F", AuctionStatus.FAILED, NOW.minusHours(12), NOW.minusDays(6), 70_000);

		em.flush();
		em.clear();
	}

	// ===== 필터 =====

	@Test
	void 카테고리를_지정하지_않으면_모든_카테고리의_경매를_조회한다() {
		Page<AuctionListResponse> page = findList(null, AuctionStatus.ACTIVE, Sort.by("endAt"));

		assertThat(titles(page)).containsExactly("A", "E", "C", "D", "B");
		assertThat(page.getTotalElements()).isEqualTo(5);
	}

	@Test
	void 카테고리를_지정하면_그_카테고리의_경매만_조회한다() {
		Page<AuctionListResponse> page = findList(clothes.getId(), AuctionStatus.ACTIVE, Sort.by("endAt"));

		assertThat(titles(page)).containsExactly("E");
		assertThat(page.getContent()).allSatisfy(item -> assertThat(item.categoryName()).isEqualTo("의류"));
	}

	@Test
	void 없는_카테고리를_지정하면_빈_목록을_돌려준다() {
		Page<AuctionListResponse> page = findList(999_999L, AuctionStatus.ACTIVE, Sort.by("endAt"));

		assertThat(page.getContent()).isEmpty();
		assertThat(page.getTotalElements()).isZero();
	}

	@Test
	void 지정한_상태의_경매만_조회한다() {
		Page<AuctionListResponse> active = findList(clothes.getId(), AuctionStatus.ACTIVE, Sort.by("endAt"));
		Page<AuctionListResponse> failed = findList(clothes.getId(), AuctionStatus.FAILED, Sort.by("endAt"));

		assertThat(titles(active)).containsExactly("E");
		assertThat(titles(failed)).containsExactly("F");
	}

	// ===== 정렬 (docs/API_컨트롤러별_작업.md: endAt,asc · createdAt,desc · currentPrice,asc|desc) =====

	@Test
	void 마감_임박순으로_정렬한다() {
		Page<AuctionListResponse> page = findList(electronics.getId(), AuctionStatus.ACTIVE,
			Sort.by(Sort.Direction.ASC, "endAt"));

		assertThat(titles(page)).containsExactly("A", "C", "D", "B");
	}

	@Test
	void 최신순으로_정렬한다() {
		Page<AuctionListResponse> page = findList(electronics.getId(), AuctionStatus.ACTIVE,
			Sort.by(Sort.Direction.DESC, "createdAt"));

		assertThat(titles(page)).containsExactly("B", "C", "D", "A");
	}

	@Test
	void 가격_높은_순으로_정렬한다() {
		Page<AuctionListResponse> page = findList(electronics.getId(), AuctionStatus.ACTIVE,
			Sort.by(Sort.Direction.DESC, "currentPrice"));

		assertThat(titles(page)).containsExactly("C", "A", "B", "D");
	}

	@Test
	void 가격_낮은_순으로_정렬한다() {
		Page<AuctionListResponse> page = findList(electronics.getId(), AuctionStatus.ACTIVE,
			Sort.by(Sort.Direction.ASC, "currentPrice"));

		assertThat(titles(page)).containsExactly("D", "B", "A", "C");
	}

	// ===== 페이징 =====

	@Test
	void 요청한_크기만큼_나눠서_조회하고_전체_건수를_알려준다() {
		PageRequest firstPage = PageRequest.of(0, 3, Sort.by("endAt"));
		PageRequest secondPage = PageRequest.of(1, 3, Sort.by("endAt"));

		Page<AuctionListResponse> first = auctionRepository.findAuctionList(electronics.getId(), AuctionStatus.ACTIVE, firstPage);
		Page<AuctionListResponse> second = auctionRepository.findAuctionList(electronics.getId(), AuctionStatus.ACTIVE, secondPage);

		assertThat(titles(first)).containsExactly("A", "C", "D");
		assertThat(first.getTotalElements()).isEqualTo(4);
		assertThat(first.getTotalPages()).isEqualTo(2);
		assertThat(first.hasNext()).isTrue();
		assertThat(titles(second)).containsExactly("B");
		assertThat(second.hasNext()).isFalse();
	}

	// ===== 응답 항목 =====

	@Test
	void 대표_이미지는_sortOrder가_가장_작은_이미지다() {
		Page<AuctionListResponse> page = findList(electronics.getId(), AuctionStatus.ACTIVE, Sort.by("endAt"));

		// 이미지를 sortOrder 2, 0, 1 순서로 넣었어도 sortOrder 0인 main이 나와야 한다
		assertThat(page.getContent())
			.extracting(AuctionListResponse::thumbnailUrl)
			.containsExactly("https://img.test/A-main.jpg", "https://img.test/C-main.jpg",
				"https://img.test/D-main.jpg", "https://img.test/B-main.jpg");
	}

	@Test
	void 목록_항목에_경매_정보가_담긴다() {
		Page<AuctionListResponse> page = findList(clothes.getId(), AuctionStatus.ACTIVE, Sort.by("endAt"));

		AuctionListResponse item = page.getContent().getFirst();
		assertThat(item.id()).isNotNull();
		assertThat(item.title()).isEqualTo("E");
		assertThat(item.categoryName()).isEqualTo("의류");
		assertThat(item.currentPrice()).isEqualTo(20_000L);
		assertThat(item.buyNowPrice()).isNull();
		assertThat(item.bidCount()).isZero();
		assertThat(item.status()).isEqualTo(AuctionStatus.ACTIVE);
		assertThat(item.endAt()).isEqualTo(NOW.plusDays(2));
	}

	// ===== 헬퍼 =====

	private Page<AuctionListResponse> findList(Long categoryId, AuctionStatus status, Sort sort) {
		return auctionRepository.findAuctionList(categoryId, status, PageRequest.of(0, 20, sort));
	}

	private List<String> titles(Page<AuctionListResponse> page) {
		return page.getContent().stream().map(AuctionListResponse::title).toList();
	}

	/**
	 * 빌더로 만들 수 없는 값(상태, 마감 시각)은 리플렉션으로 넣고,
	 * createdAt은 저장 시 Auditing이 현재 시각으로 덮어쓰므로 저장 후 SQL로 바꿉니다.
	 */
	private void saveAuction(User seller, Category category, String title, AuctionStatus status,
		LocalDateTime endAt, LocalDateTime createdAt, long price) {

		Auction auction = Auction.builder()
			.seller(seller)
			.category(category)
			.title(title)
			.description("설명")
			.startPrice(price)
			.startAt(createdAt)
			.durationDays(1)
			.extendEnabled(true)
			.extendTriggerMinutes(30)
			.extendMinutes(60)
			.build();
		ReflectionTestUtils.setField(auction, "status", status);
		ReflectionTestUtils.setField(auction, "endAt", endAt);
		ReflectionTestUtils.setField(auction, "originalEndAt", endAt);

		// 대표 이미지(sortOrder 0)를 일부러 중간에 넣는다
		auction.addImage("https://img.test/" + title + "-sub2.jpg", 2);
		auction.addImage("https://img.test/" + title + "-main.jpg", 0);
		auction.addImage("https://img.test/" + title + "-sub1.jpg", 1);

		em.persist(auction);
		em.flush();
		em.createNativeQuery("update auction set created_at = ?1 where id = ?2")
			.setParameter(1, createdAt)
			.setParameter(2, auction.getId())
			.executeUpdate();
	}
}
