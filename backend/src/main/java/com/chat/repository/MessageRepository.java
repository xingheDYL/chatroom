package com.chat.repository;

import com.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息数据访问接口
 *
 * 提供消息实体的数据库操作
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 根据房间ID查找消息，按创建时间升序排列
     */
    List<Message> findByRoomIdOrderByCreatedAtAsc(Long roomId);

    /**
     * 根据房间ID查找消息（带分页）
     */
    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId ORDER BY m.createdAt DESC")
    List<Message> findRecentByRoomId(@Param("roomId") Long roomId);

    /**
     * 统计房间内的消息数量
     */
    long countByRoomId(Long roomId);

    /**
     * 根据发送者ID查找消息
     */
    List<Message> findBySenderIdOrderByCreatedAtDesc(Long senderId);

    /**
     * 根据房间ID删除消息
     */
    void deleteByRoomId(Long roomId);
}
