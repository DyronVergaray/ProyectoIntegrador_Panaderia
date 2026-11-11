package com.panaderia.luz_maira.dao;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.Personalizado;

/** DAO de MongoDB: colección "solicitudes_personalizadas". Mismo patrón que los DAO de SQL, pero con MongoTemplate. */
@Repository
public class PersonalizadoDAO {

    private final MongoTemplate mongo;

    public PersonalizadoDAO(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Personalizado crear(Personalizado solicitud) {
        return mongo.insert(solicitud);
    }

    public Personalizado porId(String id) {
        return mongo.findById(id, Personalizado.class);
    }

    public Personalizado porIdPedido(int idPedido) {
        return mongo.findOne(Query.query(Criteria.where("idPedido").is(idPedido)), Personalizado.class);
    }

    /** Solicitudes de un cliente, las más recientes primero. */
    public List<Personalizado> deUsuario(int idUsuario) {
        return mongo.find(Query.query(Criteria.where("idUsuario").is(idUsuario))
                .with(Sort.by(Sort.Direction.DESC, "fechaCreacion")), Personalizado.class);
    }

    /** Todas (administrador), opcionalmente de un solo estado. Las pendientes de revisar van primero por fecha de entrega. */
    public List<Personalizado> todas(String estado) {
        Query consulta = new Query().with(Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        if (estado != null && !estado.isBlank()) {
            consulta.addCriteria(Criteria.where("estado").is(estado));
        }
        return mongo.find(consulta, Personalizado.class);
    }

    /**
     * Cambia el estado SOLO si la solicitud sigue en el estado esperado (así dos administradores no la aceptan a la vez).
     * Devuelve true si el cambio se hizo.
     */
    public boolean cambiarEstado(String id, String estadoEsperado, String estadoNuevo, Double precio, String nota) {
        Update cambio = new Update().set("estado", estadoNuevo).set("precio", precio).set("nota", nota);
        return mongo.updateFirst(Query.query(Criteria.where("_id").is(id).and("estado").is(estadoEsperado)),
                cambio, Personalizado.class).getModifiedCount() == 1;
    }

    public void asignarPedido(String id, int idPedido) {
        mongo.updateFirst(Query.query(Criteria.where("_id").is(id)), new Update().set("idPedido", idPedido), Personalizado.class);
    }
}
