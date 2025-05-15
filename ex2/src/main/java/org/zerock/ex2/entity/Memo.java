package org.zerock.ex2.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.persistence.Table;

@Entity
@Table(name="tbl_memo")
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor



public class Memo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long mno;
    private String memoText;
}
