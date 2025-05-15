package org.zerock.ex2.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.ex2.entity.Memo;

import java.awt.print.Pageable;
import java.util.List;

public interface MemoRepository extends JpaRepository<Memo,Long> {
    List<Memo> findByMnoBetweenOrderByMnoDesc(Long from, Long to);

    Page<Memo> findByMnoBetween(Long from, Long to, Pageable pageable);

    void deleteMemoByNoLessThan(Long num);
}
