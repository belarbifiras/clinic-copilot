-- DONNEES FICTIVES DE TEST (7 médecins) pour la base clinic_doctors.
-- À lancer une seule fois : le script est rejouable (ON CONFLICT DO NOTHING).
-- Les noms, numéros et adresses sont inventés ; seules les villes sont réelles (coordonnées approximatives).
BEGIN;

INSERT INTO doctor (id, user_id, first_name, last_name, title, bio, license_number, years_experience,
                    phone, address_line, city, governorate, postal_code, latitude, longitude,
                    verification_status, professional_status) VALUES
 ('00000000-0000-4000-8000-000000000001','test-doctor-1','Amel','Test-Dermato','Dr','DONNEES FICTIVES DE TEST','TEST-0001',8,'+216 00 000 001','1 rue Fictive','Tunis','Tunis','1000',36.8065,10.1815,'VERIFIED','ACTIVE'),
 ('00000000-0000-4000-8000-000000000002','test-doctor-2','Karim','Test-Dermato','Dr','DONNEES FICTIVES DE TEST','TEST-0002',12,'+216 00 000 002','2 rue Fictive','La Marsa','Tunis','2078',36.8782,10.3247,'VERIFIED','ACTIVE'),
 ('00000000-0000-4000-8000-000000000003','test-doctor-3','Sami','Test-Dermato','Dr','DONNEES FICTIVES DE TEST','TEST-0003',5,'+216 00 000 003','3 rue Fictive','Sousse','Sousse','4000',35.8256,10.6084,'VERIFIED','ACTIVE'),
 ('00000000-0000-4000-8000-000000000004','test-doctor-4','Nour','Test-Generaliste','Dr','DONNEES FICTIVES DE TEST','TEST-0004',10,'+216 00 000 004','4 rue Fictive','Ariana','Ariana','2080',36.8665,10.1647,'VERIFIED','ACTIVE'),
 ('00000000-0000-4000-8000-000000000005','test-doctor-5','Yassine','Test-Generaliste','Dr','DONNEES FICTIVES DE TEST','TEST-0005',15,'+216 00 000 005','5 rue Fictive','Sfax','Sfax','3000',34.7406,10.7603,'VERIFIED','ACTIVE'),
 ('00000000-0000-4000-8000-000000000006','test-doctor-6','Salma','Test-Cardio','Dr','DONNEES FICTIVES DE TEST','TEST-0006',9,'+216 00 000 006','6 rue Fictive','Tunis','Tunis','1002',36.8190,10.1658,'VERIFIED','ACTIVE'),
 ('00000000-0000-4000-8000-000000000007','test-doctor-7','Walid','Test-Pneumo','Dr','DONNEES FICTIVES DE TEST','TEST-0007',7,'+216 00 000 007','7 rue Fictive','Ben Arous','Ben Arous','2013',36.7531,10.2189,'VERIFIED','ACTIVE')
ON CONFLICT (license_number) DO NOTHING;

INSERT INTO doctor_specialty (doctor_id, specialty_id, is_primary)
SELECT v.doc::uuid, s.id, TRUE
FROM (VALUES
  ('00000000-0000-4000-8000-000000000001','DERMATOLOGY'),
  ('00000000-0000-4000-8000-000000000002','DERMATOLOGY'),
  ('00000000-0000-4000-8000-000000000003','DERMATOLOGY'),
  ('00000000-0000-4000-8000-000000000004','GENERAL_MEDICINE'),
  ('00000000-0000-4000-8000-000000000005','GENERAL_MEDICINE'),
  ('00000000-0000-4000-8000-000000000006','CARDIOLOGY'),
  ('00000000-0000-4000-8000-000000000007','PULMONOLOGY')
) AS v(doc, code)
JOIN specialty s ON s.code = v.code
ON CONFLICT DO NOTHING;

INSERT INTO doctor_language (doctor_id, language_code)
SELECT d.id, l.code
FROM doctor d CROSS JOIN (VALUES ('fr'),('ar')) AS l(code)
WHERE d.license_number LIKE 'TEST-%'
ON CONFLICT DO NOTHING;

COMMIT;