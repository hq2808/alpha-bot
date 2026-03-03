package com.alphabot.entity;

import jakarta.persistence.*;
import lombok.*;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "vn_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Represents a Vietnam stock ticker in the system catalog")
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
