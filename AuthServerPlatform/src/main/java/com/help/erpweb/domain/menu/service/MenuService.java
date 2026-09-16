package com.help.erpweb.domain.menu.service;

import com.help.erpweb.domain.content.dto.request.repository.ContentDto;
import com.help.erpweb.domain.content.entity.ContentEntity;
import com.help.erpweb.domain.content.repository.ContentRepository;
import com.help.erpweb.domain.menu.entity.MenuEntity;
import com.help.erpweb.domain.menu.projection.MenuProjection;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.erpweb.domain.menu.dto.request.repository.MenuDto;
import com.help.erpweb.domain.menu.repository.MenuRepository;
import com.help.global.authorization.Authorization;
import com.help.global.common.response.ApiResponse;
import com.help.global.data.MembershipID;
import com.help.global.jwt.CustomUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {
    private final MenuRepository menuRepository;
    private final Authorization authorization;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getMenuList(
            CustomUser user,
            int page,
            int size,
            String searchColumn,
            String searchValue,
            String sortColumn,
            String sortDirection
    ) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);

        String recommender = null;
        // 일반 사용자만 자신의 recommender를 구한다.
        // 관리자는 null → Repository에서 전체 조회
        if (!authorization.isAdmin(user)) {
            recommender = (String) entityManager
                    .createNativeQuery("""
                    SELECT Constellation_Network.search_sk(
                        :cardType,
                        :membershipId
                    )
                    """)
                    .setParameter(
                            "cardType",
                            MembershipID.discord.name()
                    )
                    .setParameter(
                            "membershipId",
                            user.getUsername()
                    )
                    .getSingleResult();
        }
        /*
         * metadata 조회
         */
        List<ColumnMetaDto> metadata =
                menuRepository.findColumnMetas(
                                MenuRepository.schemaName,
                                MenuRepository.tableName
                        )
                        .stream()
                        .map(v ->
                                ColumnMetaDto.builder()
                                        .colName(v.getColName())
                                        .isPrimary(v.getIsPrimary())
                                        .isNullable(v.getIsNullable())
                                        .build()
                        )
                        .toList();
        /*
         * metadata → 허용 컬럼
         */
        Set<String> allowedColumns =
                metadata.stream()
                        .map(ColumnMetaDto::getColName)
                        .collect(Collectors.toSet());

        Page<MenuProjection> menuPage =
                menuRepository.findPage(
                        recommender,
                        // API page는 1-based
                        // Repository는 0-based
                        normalizedPage - 1,
                        normalizedSize,
                        searchColumn,
                        searchValue,
                        sortColumn,
                        sortDirection,
                        allowedColumns
                );
        List<MenuDto> menuList =
                menuPage
                        .getContent()
                        .stream()
                        .map(entity ->
                                MenuDto.builder()
                                        .mnValue(entity.getMnValue())
                                        .recommender(entity.getRecommender())
                                        .recommenderDiscordId(
                                                entity.getRecommenderDiscordId()
                                        )
                                        .build()
                        )
                        .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("metadata", metadata);
        body.put("entities", menuList);
        body.put("page", normalizedPage);
        body.put("size", normalizedSize);
        body.put("totalElements", menuPage.getTotalElements());
        body.put("totalPages", menuPage.getTotalPages());
        body.put("hasNext", menuPage.hasNext());
        return ApiResponse.success(body);
    }

    @Transactional
    public ApiResponse<?> saveAll(CustomUser user, List<MenuDto> menuList) {
        if (menuList == null || menuList.isEmpty())
            return ApiResponse.success("No data to save");

        List<String> results = new ArrayList<>();
        for (MenuDto dto : menuList) {
            String result = menuRepository.callMenuProcedure(
                    MembershipID.discord.name(),
                    user.getUsername(),
                    dto.getMnValue()
            );
            results.add(dto.getMnValue()+" 추천 결과 : "+result);
        }
        return ApiResponse.success(results);
    }

    @Transactional
    public ApiResponse<?> deleteAll(CustomUser user, List<MenuDto> menuList) {
        if (menuList == null || menuList.isEmpty())
            return ApiResponse.success("No data to delete");

        String discordId = user.getUsername();
        String recommender = (String) entityManager
                .createNativeQuery("SELECT Constellation_Network.search_sk(:cardType, :membershipId)")
                .setParameter("cardType", MembershipID.discord.name())
                .setParameter("membershipId", discordId)
                .getSingleResult();

        // teacher가 모두 동일하다는 전제에서만 사용 가능
        List<String> values = menuList.stream()
                .map(MenuDto::getMnValue)
                .distinct()
                .toList();

        int totalCount = values.toArray().length;
        int delCount = menuRepository.deleteByMnValue(recommender, values);
        String result = "총 "+totalCount+"행 중 "+delCount+"행 삭제됨";
        return ApiResponse.success(result);
    }
}
