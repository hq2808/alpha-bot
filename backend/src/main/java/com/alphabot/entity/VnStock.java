package com.alphabot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vn_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VnStock {
    @Id
    @Column(length = 10)
    private String ticker;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(length = 100)
    private String sector;

    @Column(length = 10)
    private String exchange;
}
