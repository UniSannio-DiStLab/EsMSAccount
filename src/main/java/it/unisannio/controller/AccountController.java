package it.unisannio.controller;

import it.unisannio.model.Account;
import it.unisannio.service.BranchLocal;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Consumes("text/plain")
@Produces("text/plain")
@Path("/accounts")
public class AccountController  {

	@EJB
	private BranchLocal branch;

	@Resource UserTransaction utx; // To handle user transactions from a Web component


	public AccountController() {
		super();
	}

	@POST
	@Path("/{accountId}/deposits")
	public Response deposit(@PathParam("accountId") int accountNum, double amount) {
		try {

			branch.deposit(accountNum, amount);

			return Response.ok().build();
		} catch (Exception e) {
			System.out.println(e);
			return Response.status(500).build();
		}
	}


	@POST
	@Path("/{accountId}/withdraws")
	public Response withdraw(@PathParam("accountId") int accountNum, double amount) {
		try {
			branch.withdraw(accountNum, amount);
			return Response.ok().build();
		} catch (Exception e) {
			System.out.println(e);
			return Response.status(500).build();
		}
	}

	@GET
	@Path("/{accountId}/balance")
	public Response getBalance(@PathParam("accountId") int accountNum) {
		Account a = branch.getAccount(accountNum);
		if(a == null) return Response.status(404).build();
		try {
			return Response.ok(a.getBalance()).lastModified(a.getLastModified()).build();
		} catch (Exception e) {
			return Response.status(500).build();
		}
	}

	@PUT
	@Path("/{accountId}/balance")
	public Response setBalance(@PathParam("accountId") int accountNum, double amount, @Context Request request) {
		Account a = branch.getAccount(accountNum);
		Response.ResponseBuilder builder = null;
		try {
			//Workaround since we use Java8
			builder = request.evaluatePreconditions(Date.from(a.getLastModified().toInstant().truncatedTo(ChronoUnit.SECONDS)));
			if (builder == null) {
				utx.begin();
				branch.getAccount(accountNum).setBalance(amount);
				utx.commit();
				return Response.status(204).build();
			}
			return builder.build();
		} catch (Exception e) {
			return builder.status(500).build();
		}
	}

	@POST
	@Path("/")
	public Response createAccount(@QueryParam("cf") String custCF, double amount) {
		try {
			return Response.created(new URI("/accounts/"+branch.createAccount(custCF, amount))).build();
		} catch (Exception e) {
			return Response.status(500).build();
		}
	}

	@POST
	@Path("/transfers")
	public Response transfer(@QueryParam("source") int srcAccount, @QueryParam("destination") int dstAccount, double amount) {
		try {
			utx.begin();
			branch.getAccount(srcAccount).withdraw(amount);
			branch.getAccount(dstAccount).deposit(amount);
			utx.commit();
			return Response.ok().build();
		} catch (Exception e) {
			try {
				utx.rollback();
			} catch (SystemException ex) {
				ex.printStackTrace();
			}
			return Response.status(500).build();
		}
	}
}
