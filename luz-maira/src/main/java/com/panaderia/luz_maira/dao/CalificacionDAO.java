package com.panaderia.luz_maira.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.Calificacion;

import jakarta.annotation.PostConstruct;

/** DAO de MongoDB: colección "calificaciones". */
@Repository
public class CalificacionDAO {

    private static final Logger log = LoggerFactory.getLogger(CalificacionDAO.class);
    private final MongoTemplate mongo;

    public CalificacionDAO(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Índice único por pedido: aunque dos peticiones lleguen a la vez, solo una calificación se guarda. */
    @PostConstruct
    void crearIndice() {
        try {
            mongo.indexOps(Calificacion.class).createIndex(new Index().on("idPedido", Sort.Direction.ASC).unique());
        } catch (Exception e) {
            // Si MongoDB está apagado la aplicación igual arranca; el índice se puede crear con database/mongodb_inicial.js
            log.warn("No se pudo crear el índice de calificaciones (¿MongoDB apagado?): {}", e.getMessage());
        }
    }

    public Calificacion crear(Calificacion calificacion) {
        return mongo.insert(calificacion);       // DuplicateKeyException si el pedido ya tenía calificación
    }

    public boolean existe(int idPedido) {
        return mongo.exists(Query.query(Criteria.where("idPedido").is(idPedido)), Calificacion.class);
    }

    public List<Calificacion> deUsuario(int idUsuario) {
        return mongo.find(Query.query(Criteria.where("idUsuario").is(idUsuario)), Calificacion.class);
    }
}
