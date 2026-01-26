package com.chat.repository;

import com.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天室数据访问接口
 *
 * 提供聊天室实体的数据库操作
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 根据类型查找房间
     */
    List<ChatRoom> findByType(String type);

    /**
     * 查找由某用户创建的房间
     */
    List<ChatRoom> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    /**
     * 根据名称查找房间（模糊匹配）
     */
    List<ChatRoom> findByNameContainingIgnoreCase(String name);

    /**
     * 查找用户加入的房间
     */
    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "INNER JOIN RoomMember rm ON cr.id = rm.roomId " +
           "WHERE rm.userId = :userId " +
           "ORDER BY cr.createdAt DESC")
    List<ChatRoom> findRoomsByUserId(@Param("userId") Long userId);

    /**
     * 检查房间名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 查找两个用户之间的私聊房间
     * 查找同时包含两个指定用户的私聊类型房间
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.type = 'private' " +
           "AND cr.id IN ( " +
           "  SELECT rm1.roomId FROM RoomMember rm1 " +
           "  WHERE rm1.userId = :userId1 " +
           "  AND rm1.roomId IN ( " +
           "    SELECT rm2.roomId FROM RoomMember rm2 " +
           "    WHERE rm2.userId = :userId2 " +
           "  ) " +
           ")")
    List<ChatRoom> findPrivateChatBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
