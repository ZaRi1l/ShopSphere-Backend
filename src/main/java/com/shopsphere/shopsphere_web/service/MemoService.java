package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.MemoRepository;
import com.shopsphere.shopsphere_web.dto.Memo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemoService {

    private final MemoRepository memoRepository;

    public MemoService(MemoRepository memoRepository) {
        this.memoRepository = memoRepository;
    }

    public List<Memo> getMemo() {
        return memoRepository.findAll();
    }
}