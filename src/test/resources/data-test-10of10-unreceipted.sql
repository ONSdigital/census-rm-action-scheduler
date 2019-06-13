truncate actionv2.uac_qid_link cascade;
truncate actionv2.cases cascade;
truncate actionv2.action_rule cascade;
truncate actionv2.action_plan cascade;

INSERT INTO actionv2.cases (case_ref, abp_code, action_plan_id, address_level, address_line1, address_line2,
                            address_line3, address_type, arid, case_id, ce_expected_capacity, collection_exercise_id,
                            estab_arid, estab_type, field_coordinator_id, field_officer_id, htc_digital,
                            htc_willingness, lad, latitude, longitude, lsoa, msoa, oa, organisation_name, postcode,
                            receipt_received, region, state, town_name, treatment_code, uprn)
VALUES (10000007, 'RD06', 'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'U', 'Flat 55 Francombe House', 'Commercial Road', '',
        'HH', 'DDR190314000000195675', 'db73de73-1951-4984-bdaf-019b984c8c01', '3',
        'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'DDR190314000000113740', 'Household', '1', '2', '5', '1', 'E06000023',
        '51.4463421', '-2.5924477', 'E01014540', 'E02003043', 'E00073438', '', 'XX1 0XX', false, 'E', 'ACTIONABLE',
        'Windleybury', 'HH_LF2R3AE', '10008677190');

INSERT INTO actionv2.cases (case_ref, abp_code, action_plan_id, address_level, address_line1, address_line2,
                            address_line3, address_type, arid, case_id, ce_expected_capacity, collection_exercise_id,
                            estab_arid, estab_type, field_coordinator_id, field_officer_id, htc_digital,
                            htc_willingness, lad, latitude, longitude, lsoa, msoa, oa, organisation_name, postcode,
                            receipt_received, region, state, town_name, treatment_code, uprn)
VALUES (10000004, 'RD06', 'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'U', 'Flat 50 Francombe House', 'Commercial Road', '',
        'HH', 'DDR190314000000195675', '7a1ddad4-fbf0-4f71-b9b3-eeccc0315fb8', '3',
        'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'DDR190314000000113740', 'Household', '1', '2', '5', '1', 'E06000023',
        '51.4463421', '-2.5924477', 'E01014540', 'E02003043', 'E00073438', '', 'XX1 0XX', false, 'E', 'ACTIONABLE',
        'Windleybury', 'HH_LF3R3BE', '10008677190');

INSERT INTO actionv2.cases (case_ref, abp_code, action_plan_id, address_level, address_line1, address_line2,
                            address_line3, address_type, arid, case_id, ce_expected_capacity, collection_exercise_id,
                            estab_arid, estab_type, field_coordinator_id, field_officer_id, htc_digital,
                            htc_willingness, lad, latitude, longitude, lsoa, msoa, oa, organisation_name, postcode,
                            receipt_received, region, state, town_name, treatment_code, uprn)
VALUES (10000005, 'RD06', 'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'U', 'Flat 59 Francombe House', 'Commercial Road', '',
        'HH', 'DDR190314000000195675', '7907a8c2-7641-4121-a5ba-8a850a5d490d', '3',
        'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'DDR190314000000113740', 'Household', '1', '2', '5', '1', 'E06000023',
        '51.4463421', '-2.5924477', 'E01014540', 'E02003043', 'E00073438', '', 'XX1 0XX', false, 'E', 'ACTIONABLE',
        'Windleybury', 'HH_LF3R3AE', '10008677190');

INSERT INTO actionv2.cases (case_ref, abp_code, action_plan_id, address_level, address_line1, address_line2,
                            address_line3, address_type, arid, case_id, ce_expected_capacity, collection_exercise_id,
                            estab_arid, estab_type, field_coordinator_id, field_officer_id, htc_digital,
                            htc_willingness, lad, latitude, longitude, lsoa, msoa, oa, organisation_name, postcode,
                            receipt_received, region, state, town_name, treatment_code, uprn)
VALUES (10000006, 'RD06', 'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'U', 'Flat 53 Francombe House', 'Commercial Road', '',
        'HH', 'DDR190314000000195675', 'c4335eb3-9a46-4748-a52d-8fc5c63c2143', '3',
        'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'DDR190314000000113740', 'Household', '1', '2', '5', '1', 'E06000023',
        '51.4463421', '-2.5924477', 'E01014540', 'E02003043', 'E00073438', '', 'XX1 0XX', false, 'E', 'ACTIONABLE',
        'Windleybury', 'HH_LF2R1E', '10008677190');

INSERT INTO actionv2.cases (case_ref, abp_code, action_plan_id, address_level, address_line1, address_line2,
                            address_line3, address_type, arid, case_id, ce_expected_capacity, collection_exercise_id,
                            estab_arid, estab_type, field_coordinator_id, field_officer_id, htc_digital,
                            htc_willingness, lad, latitude, longitude, lsoa, msoa, oa, organisation_name, postcode,
                            receipt_received, region, state, town_name, treatment_code, uprn)
VALUES (10000003, 'RD06', 'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'U', 'Flat 51 Francombe House', 'Commercial Road', '',
        'HH', 'DDR190314000000195675', 'bfed70d9-4b9a-4009-8593-9ba9634b6fcb', '3',
        'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'DDR190314000000113740', 'Household', '1', '2', '5', '1', 'E06000023',
        '51.4463421', '-2.5924477', 'E01014540', 'E02003043', 'E00073438', '', 'XX1 0XX', false, 'E', 'ACTIONABLE',
        'Windleybury', 'HH_LFNR2E', '10008677190');

INSERT INTO actionv2.cases (case_ref, abp_code, action_plan_id, address_level, address_line1, address_line2,
                            address_line3, address_type, arid, case_id, ce_expected_capacity, collection_exercise_id,
                            estab_arid, estab_type, field_coordinator_id, field_officer_id, htc_digital,
                            htc_willingness, lad, latitude, longitude, lsoa, msoa, oa, organisation_name, postcode,
                            receipt_received, region, state, town_name, treatment_code, uprn)
VALUES (10000001, 'RD06', 'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'U', 'Flat 52 Francombe House', 'Commercial Road', '',
        'HH', 'DDR190314000000195675', '4a9f1302-ca30-4e74-b73d-d62a4b56bca3', '3',
        'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'DDR190314000000113740', 'Household', '1', '2', '5', '1', 'E06000023',
        '51.4463421', '-2.5924477', 'E01014540', 'E02003043', 'E00073438', '', 'XX1 0XX', false, 'E', 'ACTIONABLE',
        'Windleybury', 'HH_LFNR3AE', '10008677190');

INSERT INTO actionv2.cases (case_ref, abp_code, action_plan_id, address_level, address_line1, address_line2,
                            address_line3, address_type, arid, case_id, ce_expected_capacity, collection_exercise_id,
                            estab_arid, estab_type, field_coordinator_id, field_officer_id, htc_digital,
                            htc_willingness, lad, latitude, longitude, lsoa, msoa, oa, organisation_name, postcode,
                            receipt_received, region, state, town_name, treatment_code, uprn)
VALUES (10000000, 'RD06', 'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'U', 'Flat 56 Francombe House', 'Commercial Road', '',
        'HH', 'DDR190314000000195675', 'a6ed85c5-6cfb-4d9d-81e3-0a5f2dcc4327', '3',
        'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'DDR190314000000113740', 'Household', '1', '2', '5', '1', 'E06000023',
        '51.4463421', '-2.5924477', 'E01014540', 'E02003043', 'E00073438', '', 'XX1 0XX', false, 'E', 'ACTIONABLE',
        'Windleybury', 'HH_LF2R3BE', '10008677190');

INSERT INTO actionv2.cases (case_ref, abp_code, action_plan_id, address_level, address_line1, address_line2,
                            address_line3, address_type, arid, case_id, ce_expected_capacity, collection_exercise_id,
                            estab_arid, estab_type, field_coordinator_id, field_officer_id, htc_digital,
                            htc_willingness, lad, latitude, longitude, lsoa, msoa, oa, organisation_name, postcode,
                            receipt_received, region, state, town_name, treatment_code, uprn)
VALUES (10000009, 'RD06', 'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'U', 'Flat 58 Francombe House', 'Commercial Road', '',
        'HH', 'DDR190314000000195675', '5074b995-1ecc-464c-9e3c-7d359dbb9df2', '3',
        'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'DDR190314000000113740', 'Household', '1', '2', '5', '1', 'E06000023',
        '51.4463421', '-2.5924477', 'E01014540', 'E02003043', 'E00073438', '', 'XX1 0XX', false, 'E', 'ACTIONABLE',
        'Windleybury', 'HH_LF3R2E', '10008677190');

INSERT INTO actionv2.cases (case_ref, abp_code, action_plan_id, address_level, address_line1, address_line2,
                            address_line3, address_type, arid, case_id, ce_expected_capacity, collection_exercise_id,
                            estab_arid, estab_type, field_coordinator_id, field_officer_id, htc_digital,
                            htc_willingness, lad, latitude, longitude, lsoa, msoa, oa, organisation_name, postcode,
                            receipt_received, region, state, town_name, treatment_code, uprn)
VALUES (10000002, 'RD06', 'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'U', 'Flat 54 Francombe House', 'Commercial Road', '',
        'HH', 'DDR190314000000195675', 'bb72fc63-2db4-4e9c-94d5-7b504eeae9bf', '3',
        'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'DDR190314000000113740', 'Household', '1', '2', '5', '1', 'E06000023',
        '51.4463421', '-2.5924477', 'E01014540', 'E02003043', 'E00073438', '', 'XX1 0XX', false, 'E', 'ACTIONABLE',
        'Windleybury', 'HH_LF2R2E', '10008677190');

INSERT INTO actionv2.cases (case_ref, abp_code, action_plan_id, address_level, address_line1, address_line2,
                            address_line3, address_type, arid, case_id, ce_expected_capacity, collection_exercise_id,
                            estab_arid, estab_type, field_coordinator_id, field_officer_id, htc_digital,
                            htc_willingness, lad, latitude, longitude, lsoa, msoa, oa, organisation_name, postcode,
                            receipt_received, region, state, town_name, treatment_code, uprn)
VALUES (10000008, 'RD06', 'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'U', 'Flat 57 Francombe House', 'Commercial Road', '',
        'HH', 'DDR190314000000195675', '1e2b270a-6293-4a0a-aaed-85a7688f2edd', '3',
        'ddbd26c2-15af-4055-aab9-591b3735b8d3', 'DDR190314000000113740', 'Household', '1', '2', '5', '1', 'E06000023',
        '51.4463421', '-2.5924477', 'E01014540', 'E02003043', 'E00073438', '', 'XX1 0XX', false, 'E', 'ACTIONABLE',
        'Windleybury', 'HH_LF3R1E', '10008677190');
