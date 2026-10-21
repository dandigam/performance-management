package com.rit.performance.service;
import com.rit.performance.dto.*;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class OfficeSettingsService {
 private final OfficeLocationRepository offices;
 private final CompanySettingsRepository settings;
 public GeneralSettingsResponse general() {
  return settings.findById(1L).map(this::generalResponse).orElse(new GeneralSettingsResponse("RailInfo Tech",null));
 }
 public List<OfficeLocationResponse> list(Boolean active,String countryCode) {
  String country=countryCode==null?null:country(countryCode);
  Long mainId=general().mainOfficeId();
  return offices.findAllByOrderByCountryCodeAscOfficeNameAscIdAsc().stream()
   .filter(o->active==null || o.isActive()==active)
   .filter(o->country==null || country.equals(o.getCountryCode()))
   .map(o->response(o,mainId)).toList();
 }
 public OfficeLocationResponse get(Long id) { return response(find(id),general().mainOfficeId()); }
 @Transactional
 public OfficeLocationResponse create(OfficeLocationRequest request) {
  CompanySettings company=lockSettings();
  OfficeLocation office=new OfficeLocation();
  apply(office,request);
  offices.saveAndFlush(office);
  if(company.getMainOffice()==null && office.isActive()) company.setMainOffice(office);
  settings.saveAndFlush(company);
  return response(office,mainId(company));
 }
 @Transactional
 public OfficeLocationResponse update(Long id,OfficeLocationRequest request) {
  CompanySettings company=lockSettings();
  OfficeLocation office=find(id);
  if(Boolean.FALSE.equals(request.active()) && Objects.equals(id,mainId(company)))
   throw new InvalidOperationException("Select another active main office before deactivating this office");
  apply(office,request);
  if(company.getMainOffice()==null && office.isActive()) company.setMainOffice(office);
  offices.saveAndFlush(office);
  settings.saveAndFlush(company);
  return response(office,mainId(company));
 }
 @Transactional
 public GeneralSettingsResponse updateGeneral(GeneralSettingsRequest request) {
  CompanySettings company=lockSettings();
  OfficeLocation main=request.mainOfficeId()==null?null:find(request.mainOfficeId());
  if(main==null && offices.findAllByOrderByCountryCodeAscOfficeNameAscIdAsc().stream().anyMatch(OfficeLocation::isActive))
   throw new InvalidOperationException("mainOfficeId is required while active offices exist");
  if(main!=null && !main.isActive()) throw new InvalidOperationException("The main office must be active");
  company.setPortalName(request.portalName().trim());
  company.setMainOffice(main);
  settings.saveAndFlush(company);
  return generalResponse(company);
 }
 private CompanySettings lockSettings() {
  // Serializes main-office selection and deactivation, including concurrent first writes.
  settings.ensureSingleton();
  return settings.lockSingleton().orElseThrow(()->new IllegalStateException("Company settings unavailable"));
 }
 private OfficeLocation find(Long id) {
  return offices.findById(id).orElseThrow(()->new ResourceNotFoundException("Office location not found: "+id));
 }
 private String country(String value) {
  String code=value.trim().toUpperCase(Locale.ROOT);
  if(!Set.of(Locale.getISOCountries()).contains(code)) throw new InvalidOperationException("countryCode must be a valid ISO two-letter country code");
  return code;
 }
 private void apply(OfficeLocation office,OfficeLocationRequest r) {
  String zone=r.timeZone().trim();
  if(!ZoneId.getAvailableZoneIds().contains(zone)) throw new InvalidOperationException("timeZone must be a valid named time zone, for example America/Chicago or Asia/Kolkata");
  office.setOfficeName(r.officeName().trim()); office.setAddressLine1(r.addressLine1().trim());
  office.setAddressLine2(optional(r.addressLine2())); office.setCity(r.city().trim());
  office.setStateRegion(optional(r.stateRegion())); office.setPostalCode(optional(r.postalCode()));
  office.setCountryCode(country(r.countryCode())); office.setTimeZone(zone);
  office.setPhone(optional(r.phone())); office.setEmail(optional(r.email())); office.setActive(r.active());
 }
 private String optional(String value) { return value==null || value.isBlank()?null:value.trim(); }
 private Long mainId(CompanySettings s) { return s.getMainOffice()==null?null:s.getMainOffice().getId(); }
 private GeneralSettingsResponse generalResponse(CompanySettings s) { return new GeneralSettingsResponse(s.getPortalName(),mainId(s)); }
 private OfficeLocationResponse response(OfficeLocation o,Long mainId) {
  return new OfficeLocationResponse(o.getId(),o.getOfficeName(),o.getAddressLine1(),o.getAddressLine2(),o.getCity(),
   o.getStateRegion(),o.getPostalCode(),o.getCountryCode(),o.getTimeZone(),o.getPhone(),o.getEmail(),o.isActive(),
   Objects.equals(o.getId(),mainId),o.getCreatedOn(),o.getUpdatedOn());
 }
}
