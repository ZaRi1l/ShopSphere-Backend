package com.shopsphere.shopsphere_web;

import com.shopsphere.shopsphere_web.dto.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<Memo, Long> {
}