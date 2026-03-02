-- Vietnam Stock Catalog
CREATE TABLE vn_stocks (
    ticker       VARCHAR(10)  PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    sector       VARCHAR(100),
    exchange     VARCHAR(10)  DEFAULT 'HOSE'
);

-- ── Seed: Bluechips + Popular VN stocks ───────────────────────────────────────
INSERT INTO vn_stocks (ticker, company_name, sector, exchange) VALUES
-- Ngân hàng
('VCB',  'Ngân hàng TMCP Ngoại thương Việt Nam (Vietcombank)',      'Ngân hàng',        'HOSE'),
('BID',  'Ngân hàng TMCP Đầu tư và Phát triển VN (BIDV)',           'Ngân hàng',        'HOSE'),
('CTG',  'Ngân hàng TMCP Công thương Việt Nam (VietinBank)',         'Ngân hàng',        'HOSE'),
('MBB',  'Ngân hàng TMCP Quân đội (MB Bank)',                        'Ngân hàng',        'HOSE'),
('TCB',  'Ngân hàng TMCP Kỹ Thương Việt Nam (Techcombank)',          'Ngân hàng',        'HOSE'),
('ACB',  'Ngân hàng TMCP Á Châu',                                    'Ngân hàng',        'HOSE'),
('VPB',  'Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)',             'Ngân hàng',        'HOSE'),
('STB',  'Ngân hàng TMCP Sài Gòn Thương Tín (Sacombank)',            'Ngân hàng',        'HOSE'),
('HDB',  'Ngân hàng TMCP Phát triển TP.HCM (HDBank)',                'Ngân hàng',        'HOSE'),
('LPB',  'Ngân hàng TMCP Lộc Phát Việt Nam (LPBank)',                'Ngân hàng',        'HOSE'),
('SHB',  'Ngân hàng TMCP Sài Gòn - Hà Nội',                         'Ngân hàng',        'HNX'),
('EIB',  'Ngân hàng TMCP Xuất Nhập Khẩu Việt Nam (Eximbank)',        'Ngân hàng',        'HOSE'),
-- Công nghệ
('FPT',  'Công ty CP FPT',                                           'Công nghệ',        'HOSE'),
('CMG',  'Công ty CP Tập đoàn Công nghệ CMC',                        'Công nghệ',        'HOSE'),
('ICT',  'Tổng Công ty Giải pháp Doanh nghiệp Viettel',              'Công nghệ',        'UPCoM'),
-- Bất động sản
('VIC',  'Tập đoàn Vingroup',                                        'Bất động sản',     'HOSE'),
('VHM',  'Công ty CP Vinhomes',                                      'Bất động sản',     'HOSE'),
('NVL',  'Công ty CP Tập đoàn Đầu tư Địa ốc No Va',                 'Bất động sản',     'HOSE'),
('PDR',  'Công ty CP Phát triển BĐS Phát Đạt',                      'Bất động sản',     'HOSE'),
('KDH',  'Công ty CP Đầu tư và Kinh doanh Nhà Khang Điền',          'Bất động sản',     'HOSE'),
('DXG',  'Công ty CP Tập đoàn Đất Xanh',                            'Bất động sản',     'HOSE'),
('HDG',  'Công ty CP Tập đoàn Hà Đô',                               'Bất động sản',     'HOSE'),
-- Thép & Vật liệu
('HPG',  'Công ty CP Tập đoàn Hòa Phát',                            'Thép',             'HOSE'),
('HSG',  'Công ty CP Tập đoàn Hoa Sen',                              'Thép',             'HOSE'),
('NKG',  'Công ty CP Thép Nam Kim',                                  'Thép',             'HOSE'),
('VGS',  'Công ty CP Ống thép Việt Đức',                             'Thép',             'HNX'),
-- Dầu khí
('GAS',  'Tổng Công ty Khí Việt Nam (PV GAS)',                       'Dầu khí',          'HOSE'),
('PVD',  'Công ty CP PV Drilling',                                   'Dầu khí',          'HOSE'),
('PLX',  'Tập đoàn Xăng Dầu Việt Nam (Petrolimex)',                  'Dầu khí',          'HOSE'),
('PVS',  'Tổng Công ty CP Dịch vụ Kỹ thuật Dầu khí',               'Dầu khí',          'HNX'),
-- Hàng tiêu dùng
('VNM',  'Công ty CP Sữa Việt Nam (Vinamilk)',                       'Hàng tiêu dùng',   'HOSE'),
('SAB',  'Tổng Công ty CP Bia - Rượu - NGK Sài Gòn (Sabeco)',        'Hàng tiêu dùng',   'HOSE'),
('MSN',  'Công ty CP Tập đoàn Masan',                                'Hàng tiêu dùng',   'HOSE'),
('MWG',  'Công ty CP Đầu tư Thế Giới Di Động',                      'Bán lẻ',           'HOSE'),
('PNJ',  'Công ty CP Vàng bạc Đá quý Phú Nhuận',                    'Bán lẻ',           'HOSE'),
-- Điện & Năng lượng
('POW',  'Tổng Công ty Điện lực Dầu khí Việt Nam (PV Power)',        'Điện',             'HOSE'),
('NT2',  'Công ty CP Điện lực Dầu khí Nhơn Trạch 2',                'Điện',             'HOSE'),
('PC1',  'Công ty CP Xây lắp Điện I',                                'Điện',             'HOSE'),
('EVF',  'Công ty TC TNHH MTV Tài chính Điện lực',                   'Điện',             'UPCoM'),
-- Hàng không & Vận tải
('HVN',  'Tổng Công ty Hàng không Việt Nam (Vietnam Airlines)',       'Hàng không',       'HOSE'),
('VJC',  'Công ty CP Hàng không VietJet',                            'Hàng không',       'HOSE'),
('GMD',  'Công ty CP Gemadept',                                      'Logistics',        'HOSE'),
-- Bảo hiểm & Chứng khoán
('BVH',  'Tập đoàn Bảo Việt',                                        'Bảo hiểm',        'HOSE'),
('VND',  'Công ty CP Chứng khoán VNDirect',                          'Chứng khoán',      'HOSE'),
('SSI',  'Công ty CP Chứng khoán SSI',                               'Chứng khoán',      'HOSE'),
('HCM',  'Công ty CP Chứng khoán TP. Hồ Chí Minh (HSC)',            'Chứng khoán',      'HOSE'),
('VCI',  'Công ty CP Chứng khoán Bản Việt',                         'Chứng khoán',      'HOSE'),
-- Xây dựng & Hạ tầng
('CTD',  'Công ty CP Xây dựng Coteccons',                            'Xây dựng',         'HOSE'),
('VCG',  'Tổng Công ty CP Xuất nhập khẩu Xây dựng Việt Nam',        'Xây dựng',         'HOSE'),
('DPM',  'Tổng Công ty Phân bón và Hóa chất Dầu khí (PVFCCo)',      'Hóa chất',         'HOSE'),
('DCM',  'Công ty CP Phân bón Dầu khí Cà Mau',                      'Hóa chất',         'HOSE'),
-- Dược phẩm
('DHG',  'Công ty CP Dược Hậu Giang',                                'Dược phẩm',        'HOSE'),
('IMP',  'Công ty CP Dược phẩm Imexpharm',                           'Dược phẩm',        'HOSE'),
('TRA',  'Công ty CP Traphaco',                                      'Dược phẩm',        'HOSE'),
-- Nông nghiệp & Thủy sản
('VHC',  'Công ty CP Vĩnh Hoàn',                                     'Thủy sản',         'HOSE'),
('ANV',  'Công ty CP Nam Việt',                                      'Thủy sản',         'HOSE'),
('IDI',  'Công ty CP Đầu tư và Phát triển Đa Quốc Gia I.D.I',      'Thủy sản',         'HOSE'),
-- Cao su
('GVR',  'Tập đoàn Công nghiệp Cao su Việt Nam',                     'Cao su',           'HOSE')
ON CONFLICT (ticker) DO NOTHING;
