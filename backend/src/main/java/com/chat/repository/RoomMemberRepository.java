package com.chat.repository;

import com.chat.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 聊天室成员数据访问接口
 *
 * 提供聊天室成员实体的数据库操作
 */
@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    /**
     * 查找房间的所有成员
     */
    List<RoomMember> findByRoomId(Long roomId);

    /**
     * 查找用户加入的所有房间
     */
    List<RoomMember> findByUserId(Long userId);

    /**
     * 查找特定房间的特定成员
     */
    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 检查用户是否是房间成员
     */
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 统计房间成员数量
     */
    long countByRoomId(Long roomId);

    /**
     * 将用户从房间中删除
     */
    void deleteByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 查找房间成员（包含用户详情）
     */
    @Query("SELECT rm FROM RoomMember rm " +
           "WHERE rm.roomId = :roomId " +
           "ORDER BY rm.joinedAt ASC")
    List<RoomMember> findRoomMembersWithDetails(@Param("roomId") Long roomId);

    /**
     * 查找用户的所有房间
     */
    @Query("SELECT rm FROM RoomMember rm " +
           "WHERE rm.userId = :userId " +
           "ORDER BY rm.joinedAt DESC")
    List<RoomMember> findAllRoomsForUser(@Param("userId") Long userId);
}
