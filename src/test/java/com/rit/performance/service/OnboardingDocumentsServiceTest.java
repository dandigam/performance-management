package com.rit.performance.service;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class OnboardingDocumentsServiceTest {
 @Test void savesOwnUploadsRejectsOthersAndRemovesUnselectedLinks() {
  var current=mock(CurrentEmployeeService.class); var onboardings=mock(EmployeeOnboardingRepository.class);
  var documents=mock(DocumentRepository.class); var lookups=mock(LookupValueRepository.class);
  var employees=mock(EmployeeRepository.class); var self=mock(OnboardingSelfService.class);
  var employee=new Employee(); employee.setId(10L); employee.setWorkMode("OFFSHORE");
  var onboarding=new EmployeeOnboarding(); onboarding.setStatus("IN_PROGRESS");
  when(current.currentEmployee()).thenReturn(employee);
  when(onboardings.findForUpdateByEmployeeId(10L)).thenReturn(Optional.of(onboarding));
  var type=LookupValue.builder().id(1L).code("RESUME").build();
  when(lookups.findByLookupTypeCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrueOrderByDisplayOrderAscIdAsc(anyString())).thenReturn(List.of(type));
  var document=Document.builder().id(20L).module("EMPLOYEE").documentType("RESUME").createdBy(7L).build();
  when(documents.findAllById(Set.of(20L))).thenReturn(List.of(document));
  var service=new OnboardingDocumentsService(current,onboardings,documents,lookups,employees,self);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
    new AuthenticatedUser(7L,"test","",true,0,List.of()),null,List.of()));
  try {
   service.update(List.of(20L)); service.update(List.of(20L));
   assertThat(employee.getEmployeeDocuments()).hasSize(1);
   verify(employees,times(2)).saveAndFlush(employee);
   var foreign=Document.builder().id(21L).module("EMPLOYEE").documentType("RESUME").createdBy(8L).build();
   when(documents.findAllById(Set.of(21L))).thenReturn(List.of(foreign));
   assertThatThrownBy(()->service.update(List.of(21L))).hasMessageContaining("your own");
   assertThat(employee.getEmployeeDocuments()).hasSize(1);
   service.update(List.of()); assertThat(employee.getEmployeeDocuments()).isEmpty();
   onboarding.setStatus("SUBMITTED");
   assertThatThrownBy(()->service.update(List.of())).hasMessageContaining("stage");
  } finally { SecurityContextHolder.clearContext(); }
 }
}
