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
import org.ejml.sparse.csc.CommonOps_DSCC;

import java.util.Arrays;

import static org.ejml.sparse.csc.mult.ImplSparseSparseMult_DSCC.multAddColA;

/**
 * Implementation class.  Not recommended for direct use.  Instead use {@link CommonOps_DSCC}
 * instead.
 *
 * @author Peter Abeles
 */
public class ImplCommonOps_DSCC {

    /**
     * Performs a matrix transpose.
     *
     * @param A Original matrix.  Not modified.
     * @param C Storage for transposed 'a'.  Assumed to be of the correct shape and length.
     * @param work Work space.  null or an array the size of the rows in 'a'
     */
    public static void transpose(DMatrixSparseCSC A , DMatrixSparseCSC C , int work[] ) {
        work = checkDeclare(A.numRows, work, true);
        C.nz_length = A.nz_length;

        // compute the histogram for each row in 'a'
        int idx0 = A.col_idx[0];
        for (int j = 1; j <= A.numCols; j++) {
            int idx1 = A.col_idx[j];
            for (int i = idx0; i < idx1; i++) {
                work[A.nz_rows[i]]++;
            }
            idx0 = idx1;
        }

        // construct col_idx in the transposed matrix
        C.colsum(work);

        // fill in the row indexes
        idx0 = A.col_idx[0];
        for (int j = 1; j <= A.numCols; j++) {
            int col = j-1;
            int idx1 = A.col_idx[j];
            for (int i = idx0; i < idx1; i++) {
                int row = A.nz_rows[i];
                int index = work[row]++;
                C.nz_rows[index] = col;
                C.nz_values[index] = A.nz_values[i];
            }
            idx0 = idx1;
        }
    }

    /**
     * Performs matrix addition:<br>
     * C = &alpha;A + &beta;B
     *
     * @param alpha scalar value multiplied against A
     * @param A Matrix
     * @param beta scalar value multiplied against B
     * @param B Matrix
     * @param C Output matrix.
     * @param w (Optional) Work space of length A.rows.  Null to declare internally
     * @param x (Optional) Work space of length A.rows.  Null to declare internally
     */
    public static void add(double alpha, DMatrixSparseCSC A, double beta, DMatrixSparseCSC B, DMatrixSparseCSC C,
                           int w[], double x[])
    {
        x = checkDeclare(A.numRows, x);
        w = checkDeclare(A.numRows, w, true);

        C.indicesSorted = false;
        C.nz_length = 0;

        for (int col = 0; col < A.numCols; col++) {
            C.col_idx[col] = C.nz_length;

            multAddColA(A,col,alpha,C,col,x,w);
            multAddColA(B,col,beta,C,col,x,w);

            // take the values in the dense vector 'x' and put them into 'C'
            int idxC0 = C.col_idx[col];
            int idxC1 = C.col_idx[col+1];

            for (int i = idxC0; i < idxC1; i++) {
                C.nz_values[i] = x[C.nz_rows[i]];
            }
        }
    }

    public static int[] checkDeclare( int N, int[] w, boolean fillZeros) {
        if( w == null )
            w = new int[N];
        else if( w.length < N )
            throw new IllegalArgumentException("w needs to at least be as long as A.numRows");
        else if( fillZeros )
            Arrays.fill(w,0,N,0);
        return w;
    }

    public static int[] checkDeclare( int N, int[] w, int fillToM ) {
        if( w == null )
            w = new int[N];
        else if( w.length < N )
            throw new IllegalArgumentException("w needs to at least be as long as A.numRows");
        else if( fillToM > 0 )
            Arrays.fill(w,0,fillToM,0);
        return w;
    }

    public static double[] checkDeclare( int N, double[] x) {
        if( x == null )
            x = new double[N];
        else if( x.length < N )
            throw new IllegalArgumentException("x needs to at least be as long as A.numRows");
        return x;
    }
}
