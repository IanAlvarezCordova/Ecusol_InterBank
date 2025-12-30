package com.estructura.cbs.config;

import com.estructura.cbs.model.Sucursal;
import com.estructura.cbs.repository.SucursalRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class MongoDataSeeder implements CommandLineRunner {

        private final SucursalRepository sucursalRepo;

        public MongoDataSeeder(SucursalRepository sucursalRepo) {
                this.sucursalRepo = sucursalRepo;
        }

        @Override
        public void run(String... args) throws Exception {
                long count = sucursalRepo.count();
                System.out.println(">>> MONGO DATA SEEDER: Current Sucursal Count = " + count);
                if (count == 0) {
                        System.out.println(">>> SEEDING MONGODB WITH SUCURSALES...");
                        List<Sucursal> sucursales = Arrays.asList(
                                        crear(1, "SUC-001", "Sucursal Matriz", "Av. Amazonas y Naciones Unidas",
                                                        "-0.180653", "-78.467834",
                                                        "022222222"),
                                        crear(2, "SUC-002", "Sucursal Centro", "Calle Guayaquil y Chile", "-0.220653",
                                                        "-78.513834",
                                                        "022333333"),
                                        crear(3, "SUC-003", "Sucursal Sur", "Av. Teniente Hugo Ortiz", "-0.280653",
                                                        "-78.537834",
                                                        "022444444"),
                                        crear(4, "SUC-004", "Sucursal Cumbayá", "Av. Interoceánica", "-0.198653",
                                                        "-78.432834",
                                                        "022555555"),
                                        crear(5, "SUC-005", "Sucursal Guayaquil Centro", "Av. 9 de Octubre",
                                                        "-2.190653", "-79.887834",
                                                        "042222222"),
                                        crear(6, "SUC-006", "Sucursal Guayaquil Norte", "Av. Francisco de Orellana",
                                                        "-2.160653",
                                                        "-79.917834", "042333333"),
                                        crear(7, "SUC-007", "Sucursal Cuenca", "Calle Gran Colombia", "-2.900653",
                                                        "-79.007834",
                                                        "072222222"),
                                        crear(8, "SUC-008", "Sucursal Ambato", "Av. Cevallos", "-1.240653",
                                                        "-78.627834", "032222222"));
                        sucursalRepo.saveAll(sucursales);
                        System.out.println(">>> SUCURSALES CREADAS: " + sucursales.size());
                }
        }

        private Sucursal crear(Integer id, String codigo, String nombre, String direccion, String lat, String lng,
                        String telefono) {
                Sucursal s = new Sucursal();
                s.setSucursalId(id);
                s.setCodigoSucursal(codigo);
                s.setNombre(nombre);
                s.setDireccion(direccion);
                s.setEntidadId(1); // Default Entidad
                s.setEstado("ACTIVA");
                try {
                        s.setLatitud(new java.math.BigDecimal(lat));
                        s.setLongitud(new java.math.BigDecimal(lng));
                } catch (Exception e) {
                        s.setLatitud(java.math.BigDecimal.ZERO);
                        s.setLongitud(java.math.BigDecimal.ZERO);
                }
                s.setTelefono(telefono);
                return s;
        }
}
