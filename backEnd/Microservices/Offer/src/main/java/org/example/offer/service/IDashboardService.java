package org.example.offer.service;

import org.example.offer.dto.response.DashboardStatsResponse;
import org.example.offer.dto.response.OfferStatsResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interface du service métier du dashboard et des statistiques (gestion Offer).
 * Définit les méthodes métier et avancées pour les tableaux de bord freelancer et admin.
 */
public interface IDashboardService {

    // ========== Méthodes métier – Dashboard freelancer ==========

    /**
     * Récupérer les statistiques du dashboard pour un freelancer (contrats actifs,
     * dépenses 30 jours, projets publiés, candidatures en attente, offres actives).
     */
    DashboardStatsResponse getFreelancerDashboardStats(Long freelancerId);

    /**
     * Récupérer les statistiques détaillées des offres d'un freelancer (totaux par statut,
     * revenus, vues, candidatures, évolution mensuelle/hebdomadaire).
     */
    OfferStatsResponse getOfferStatsByFreelancer(Long freelancerId);

    /**
     * Nombre de candidatures en attente pour les offres d'un freelancer.
     */
    Long getPendingApplicationsCountByFreelancer(Long freelancerId);

    // ========== Méthodes avancées – Statistiques sur période ==========

    /**
     * Statistiques du dashboard pour un freelancer sur une période donnée.
     */
    DashboardStatsResponse getFreelancerDashboardStatsForPeriod(Long freelancerId, LocalDate startDate, LocalDate endDate);

    /**
     * Revenus (offres acceptées) par mois pour une année donnée.
     * Clé = numéro de mois (1-12), Valeur = somme des prix.
     */
    Map<Integer, BigDecimal> getRevenueByMonth(Long freelancerId, int year);

    /**
     * Évolution du nombre de candidatures reçues par jour sur les N derniers jours.
     * Utile pour graphiques de tendance.
     */
    Map<LocalDate, Long> getApplicationTrendByFreelancer(Long freelancerId, int lastDays);

    /**
     * Top N offres du freelancer par nombre de vues.
     */
    List<Long> getTopOfferIdsByViews(Long freelancerId, int limit);

    // ========== Méthodes avancées – Dashboard admin (global) ==========

    /**
     * Statistiques globales pour l'admin : total offres, total candidatures,
     * offres par statut, répartition par domaine/catégorie.
     */
    Map<String, Object> getGlobalStatsForAdmin();
}
