/*
 * Copyright (c) 2009-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.sparse.csc.misc;

import org.ejml.data.DMatrixSparseCSC;

import java.util.Arrays;

import static org.ejml.sparse.csc.misc.ImplCommonOps_DSCC.checkDeclare;

/**
 * @author Peter Abeles
 */
public class TriangularSolver_DSCC {

    /**
     * Solves for a lower triangular matrix against a dense matrix. L*x = b
     *
     * @param L Lower triangular matrix.  Diagonal elements are assumed to be non-zero
     * @param x (Input) Solution matrix 'b'.  (Output) matrix 'x'
     */
    public static void solveL(DMatrixSparseCSC L , double []x )
    {
        final int N = L.numCols;

        int idx0 = L.col_idx[0];
        for (int col = 0; col < N; col++) {

            int idx1 = L.col_idx[col+1];
            double x_j = x[col] /= L.nz_values[idx0];

            for (int i = idx0+1; i < idx1; i++) {
                int row = L.nz_rows[i];
                x[row] -=  L.nz_values[i]*x_j;
            }

            idx0 = idx1;
        }
    }

    /**
     * Solves for an upper triangular matrix against a dense vector. L*x = b
     *
     * @param U Upper triangular matrix.  Diagonal elements are assumed to be non-zero
     * @param x (Input) Solution matrix 'b'.  (Output) matrix 'x'
     */
    public static void solveU(DMatrixSparseCSC U , double []x )
    {
        final int N = U.numCols;

        int idx1 = U.col_idx[N];
        for (int col = N-1; col >= 0; col--) {
            int idx0 = U.col_idx[col];
            double x_j = x[col] /= U.nz_values[idx1-1];

            for (int i = idx0; i < idx1-1; i++) {
                int row = U.nz_rows[i];
                x[row] -=  U.nz_values[i]*x_j;
            }

            idx1 = idx0;
        }
    }

    /**
     * Computes the solution to the triangular system.  Inputs are both sparse but the output is dense.
     *
     * @param G (Input) Lower or upper triangular matrix.  diagonal elements must be non-zero.  Not modified.
     * @param lower true for lower triangular and false for upper
     * @param B (Input) Matrix.  Not modified.
     * @param X (Output) Solution
     * @param x (Optional) Storage for work space.  length = G.numRows
     * @param xi (Optional) Storage for work space.  length = B.numRows
     * @param w (Optional) Storage for work space.  length = B.numRows*2
     */
    public static void solve(DMatrixSparseCSC G, boolean lower,
                             DMatrixSparseCSC B, DMatrixSparseCSC X, double x[], int xi[], int w[])
    {
        x = checkDeclare(G.numRows,x);
        xi = checkDeclare(G.numRows,xi,false);
        w = checkDeclare(B.numRows*2,w,B.numRows);

        X.nz_length = 0;
        X.col_idx[0] = 0;
        X.indicesSorted = false;

        for (int colB = 0; colB < B.numCols; colB++) {
            int top = solve(G,lower,B,colB, x, xi, w);

            int nz_count = X.numRows-top;
            if( X.nz_values.length < X.nz_length + nz_count) {
                X.growMaxLength(X.nz_length*2 + nz_count,true);
            }

            for (int p = top; p < X.numRows; p++,X.nz_length++) {
                X.nz_rows[X.nz_length] = xi[p];
                X.nz_values[X.nz_length] = x[xi[p]];
            }
            X.col_idx[colB+1] = X.nz_length;
        }
    }

    /**
     * Computes the solution to a triangular system.  Only a single column in B is solved for.
     *
     * @param G (Input) Lower or upper triangular matrix.  diagonal elements must be non-zero.  Not modified.
     * @param lower true for lower triangular and false for upper
     * @param B (Input) Matrix.  Not modified.
     * @param colB The column in B which is solved for
     * @param x (Output) Storage for dense solution.  length = G.numRows
     * @param xi (Optional) Storage for work space.  length = B.numRows
     * @param w (Optional) Storage for work space.  length = B.numRows(2
     * @return Return number of zeros in 'x', ignoring cancellations.
     */
    public static int solve(DMatrixSparseCSC G, boolean lower,
                            DMatrixSparseCSC B, int colB, double x[], int xi[], int w[])
    {
        xi = checkDeclare(G.numRows,xi,false);
        w = checkDeclare(B.numRows*2,w,B.numRows);

        int top = searchNzRowsInB(G,B,colB,xi,w);

        // sparse clear of x
        for( int p = top; p < G.numRows; p++ )
            x[xi[p]] = 0;

        // copy B into X
        int idxB0 = B.col_idx[colB];
        int idxB1 = B.col_idx[colB+1];
        for( int p = idxB0; p < idxB1; p++ )
            x[B.nz_rows[p]] = B.nz_values[p];

        for (int px = top; px < G.numRows; px++) {
            int j = xi[px];
            if( j < 0 )
                continue;
            int p,q;
            if( lower ) {
                x[j] /= G.nz_values[G.col_idx[j]];
                p = G.col_idx[j]+1;
                q = G.col_idx[j+1];
            } else {
                x[j] /= G.nz_values[G.col_idx[j+1]-1];
                p = G.col_idx[j];
                q = G.col_idx[j+1]-1;
            }
            for(;p<q;p++) {
                x[G.nz_rows[p]] -= G.nz_values[p]*x[j];
            }
        }

        return top;
    }

    /**
     * <p>Determines which elements in 'X' will be non-zero when the system below is solved for.</p>
     * G*X = B
     *
     * <p>xi will contain a list of ordered row indexes in B which will be modified starting at xi[top] to xi[n-1].  top
     * is the value returned by this function.</p>
     *
     * <p>See cs_reach in dsparse library to understand the algorithm.  This code follow the spirit but not
     * the details because of differences in the contract.</p>
     *
     * @param G (Input) Lower triangular system matrix.  Diagonal elements are assumed to be not zero.  Not modified.
     * @param B (Input) Matrix B. Not modified.
     * @param colB Column in B being solved for
     * @param xi (Output) List of row indices in B which are non-zero in graph order.  Must have length B.numRows
     * @param w Work space array used internally.  Must be of size B.numRows*2
     * @return Returns the index of the first element in the xi list.  Also known as top.
     */
    public static int searchNzRowsInB(DMatrixSparseCSC G , DMatrixSparseCSC B , int colB, int xi[] , int w[])
    {
        if( xi.length < B.numRows )
            throw new IllegalArgumentException("xi must be at least this long: "+B.numRows);
        if( w.length < B.numRows*2 )
            throw new IllegalArgumentException("xi must be at least this long: "+B.numRows*2);
        Arrays.fill(w,0,B.numRows,0);

        // use 'w' as a marker to know which rows in B have been examined.  0 = unexamined and 1 = examined
        int idx0 = B.col_idx[colB];
        int idx1 = B.col_idx[colB+1];

        int top = G.numRows;
        for (int i = idx0; i < idx1; i++) {
            int rowB = B.nz_rows[i];

            if( w[rowB] == 0 ) {
                top = searchNzRowsInB_DFS(rowB,G,top,xi,w);
            }
        }

        return top;
    }

    /**
     * Given the first row in B it performs a DFS seeing which elements in 'B' will be not zero.  A row=i in 'B' will
     * be not zero if any element in row=(j < i) in G is not zero
     */
    private static int searchNzRowsInB_DFS(int rowB , DMatrixSparseCSC G , int top , int xi[], int w[] )
    {
        int N = G.numRows;
        int head = 0; // put the selected row into the FILO stack
        xi[head] = rowB; // use the head of xi to store where the stack it's searching.  The tail is where
                         // the graph ordered list of rows in B is stored.
        while( head >= 0 ) {
            // the column in G being examined
            int G_col = xi[head];

            if( w[G_col] == 0) {
                w[G_col] = 1;
                w[N+head] = G.col_idx[G_col]; // mark which child in the loop below it's examining
            }

            // See if there are any children which have yet to be examined
            boolean done = true;

            int idx0 = w[N+head];
            int idx1 = G.col_idx[G_col+1];

            for (int j = idx0; j < idx1; j++) {
                int jrow = G.nz_rows[j];
                if( w[jrow] == 0 ) {
                    w[N+head] = j+1; // mark that it has processed up to this point
                    xi[++head] = jrow;
                    done = false;
                    break;          // It's a DFS so break and continue down
                }
            }

            if( done ) {
                head--;
                xi[--top] = G_col;
            }

        }
        return top;
    }

    /**
     * <p>Computes the elimination tree for sparse lower triangular square matrix generated from cholesky (ata=false)
     * and BLAh (ata=true) decompositions. In an elimination tree the parent of node 'i' is 'j', where the
     * first off-diagonal non-zero in column 'i' has row index 'j'; j > i for which l[k,i] != 0.</p>
     *
     * <p>This tree encodes the non-zero elements in L given A, e.g. L*L' = A, and enables faster to compute solvers
     * than the general purpose implementations.</p>
     *
     * <p>Functionally identical to cs_etree in csparse</p>
     *
     * @param A (Input) M by N sparse upper triangular matrix.  If ata is false then M=N otherwise M>N
     * @param ata If true then it computes elimination treee of A'A without forming A'A otherwise computes elimination
     *            tree for cholesky factorization
     * @param parent (Output) Parent of each node in tree.  -1 if no parent.  Size N.
     * @param work (Optional) Work space.  can be null.  size N+M if ata = true or N if false.
     */
    public static void eliminationTree( DMatrixSparseCSC A , boolean ata , int parent[] , int work[]) {
        int m = A.numRows;
        int n = A.numCols;

        if( parent.length < n )
            throw new IllegalArgumentException("parent must be of length N");

        if( work == null )
            work = new int[n + (ata ? m : 0)];

        int ancestor = 0; // reference to index in work array
        int previous = n; // reference to index in work array

        if( ata ) {
            for (int i = 0; i < m; i++) {
                work[previous+i] = -1;
            }
        }

        // step through each column
        for (int k = 0; k < n; k++) {
            parent[k] = -1;
            work[ancestor+k] = -1;

            int idx0 = A.col_idx[k];   // node k has no parent
            int idx1 = A.col_idx[k+1]; // node k has no ancestor

            for (int p = idx0; p < idx1; p++) {

                int nz_row_p = A.nz_rows[p];

                int i = ata ? work[previous+nz_row_p] : nz_row_p;

                int inext;
                while( i != -1 && i < k ) {
                    inext = work[ancestor+i];
                    work[ancestor+i] = k;
                    if( inext == -1 ) {
                        parent[i] = k;
                        break;
                    } else {
                        i = inext;
                    }
                }

                if( ata ) {
                    work[previous+nz_row_p] = k;
                }
            }
        }
    }

    /**
     * <p>Given an elimination tree compute the non-zero elements in the specified row of L given the
     * symmetric A matrix.  This is in general much faster than general purpose algorithms</p>
     *
     * <p>Functionally equivalent to cs_ereach() in csparse</p>
     *
     * @param A Symmetric matrix.
     * @param k Row in A being processed.
     * @param parent elimination tree.
     * @param s (Output) s[top:(n-1)] = pattern of L[k,:].  Must have length A.numCols
     * @param w Work space array used internally.  All elements must be >= 0 on input. Must be of size A.numCols
     * @return Returns the index of the first element in the xi list.  Also known as top.
     */
    public static int searchNzRowsElim( DMatrixSparseCSC A , int k , int parent[], int s[], int w[] ) {
        int top = A.numCols;

        // Traversing through the column in A is the same as the row in A since it's symmetric
        int idx0 = A.col_idx[k], idx1 = A.col_idx[k+1];

        w[k] = -w[k]-2;  // makr node k as visited
        for (int p = idx0; p < idx1; p++) {
            int i = A.nz_rows[p];   // A[k,i] is not zero

            if( i > k ) // only consider upper triangular part of A
                continue;

            // move up the elimination tree
            int len = 0;
            for(;w[i]>=0; i = parent[i]) {
                s[len++] = i; // L[k,i] is not zero
                w[i] = -w[i]-2; // mark i as being visited
            }
            while( len > 0 ) {
                s[--top] = s[--len];
            }
        }

        // unmark all nodes
        for( int p = top; p < A.numCols; p++ ) {
            w[s[p]] = -w[s[p]]-2;
        }
        w[k] = -w[k]-2;
        return top;
    }

}
