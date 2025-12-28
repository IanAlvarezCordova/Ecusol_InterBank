package com.ecusol.ms_clientes.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ecusol.ms_clientes.dto.PersonaDTO;
import com.ecusol.ms_clientes.dto.RegistroClientePersonaDTO;
import com.ecusol.ms_clientes.model.Persona;

import java.time.LocalDate;

@Mapper(componentModel = "spring", imports = LocalDate.class)
public interface PersonaMapper {

    @Mapping(target = "tipoCliente", constant = "P")
    @Mapping(target = "estado", constant = "ACTIVO")
    @Mapping(target = "fechaRegistro", expression = "java(LocalDate.now())")
    @Mapping(target = "numeroIdentificacion", source = "cedula")
    @Mapping(target = "tipoIdentificacion", constant = "CEDULA")
    Persona toEntity(RegistroClientePersonaDTO dto);

    PersonaDTO toDTO(Persona persona);
}
